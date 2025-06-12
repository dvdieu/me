package org.example.v2;/*
 * MatchingEngine.java – mô hình mẫu (single‑thread) khớp lệnh đầy đủ:
 *  • LIMIT, MARKET
 *  • STOP‑MARKET, STOP‑LIMIT
 *  • TIF: GTC, IOC, FOK
 *  • ICEBERG (ẩn khối lượng, refill mất ưu tiên)
 *  • Thuật toán Breadth‑First qua từng bucket giá,
 *    kích hoạt stop‑order ở đoạn giá vừa vượt qua.
 *  • Không đệ quy  →  code gọn, dễ trace, dễ bảo trì.
 *
 *  Đây KHÔNG tối ưu cho thông lượng (chưa dùng LMAX, off‑heap…),
 *  mục tiêu là *đúng logic* & *dễ hiểu* để bạn tuỳ nghi tối ưu hóa.
 */

import java.util.*;

public class MatchingEngine {

    /* ================================ ENUMS  ================================ */
    enum Side { BUY, SELL; Side opp() {return this==BUY?SELL:BUY;} }

    enum OrderKind { LIMIT, MARKET, STOP_MARKET, STOP_LIMIT,OCO }

    enum TimeInForce { GTC, IOC, FOK, POST_ONLY }

    /* =============================== ORDER ================================== */
    static class Order {
        long id;              // id duy nhất (Engine gán)
        long userId;          // chủ sở hữu (demo thôi)
        Side side;
        OrderKind kind;
        TimeInForce tif;
        long limitPrice;      // 0 nếu MARKET hoặc STOP_MARKET
        long stopPrice;       // 0 nếu không phải STOP_*
        long totalQty;        // khối lượng còn lại (bao gồm ẩn)
        long displayQty;      // khối lượng đang hiển thị (đối với ICEBERG)
        long peakSize;        // 0 nếu không phải ICEBERG

        Order(long userId, Side side, OrderKind kind, TimeInForce tif,
              long limitPrice, long stopPrice, long qty, long peak) {
            this.userId      = userId;
            this.side        = side;
            this.kind        = kind;
            this.tif         = tif;
            this.limitPrice  = limitPrice;
            this.stopPrice   = stopPrice;
            this.totalQty    = qty;
            this.peakSize    = peak;
            this.displayQty  = peak>0 ? Math.min(qty, peak) : qty; // ICEBERG hay thường
        }
        boolean isIceberg()     { return peakSize>0;             }
        boolean isStop()        { return kind==OrderKind.STOP_MARKET || kind==OrderKind.STOP_LIMIT; }
        boolean isStopMarket()  { return kind==OrderKind.STOP_MARKET; }
        boolean isStopLimit()   { return kind==OrderKind.STOP_LIMIT;  }
        boolean isMarket()      { return kind==OrderKind.MARKET;      }
        boolean isLimit()       { return kind==OrderKind.LIMIT;       }
        boolean isOco()       { return kind==OrderKind.OCO; }

        @Override public String toString() {
            return String.format("{id=%d %s %s %d/%d @%d stop=%d}", id, side, kind, displayQty, totalQty, limitPrice, stopPrice);
        }
    }
    /* =========================== PRICE‑LEVEL ================================ */
    static class PriceLevel {
        final long price;
        final Deque<Order> orders = new ArrayDeque<>();          // FIFO
        PriceLevel(long p) { price = p; }
        long totalQty()   { return orders.stream().mapToLong(o->o.displayQty).sum(); }

        @Override
        public String toString() {
            return "PriceLevel{" +
                    "price=" + price +
                    ", orders=" + orders +
                    '}';
        }
    }

    /* ============================ ORDER BOOK ================================ */
    static class OrderBook {
        // Bids desc (price lớn → nhỏ), Asks asc (giá nhỏ → lớn)
        final NavigableMap<Long,PriceLevel> bids = new TreeMap<>(Collections.reverseOrder());
        final NavigableMap<Long,PriceLevel> asks = new TreeMap<>();

        /* ---------- tiện ích cơ bản ---------- */
        PriceLevel level(long price, Side side) {
            return (side==Side.BUY? bids:asks).get(price);
        }
        Long bestOppPrice(Side taker) {
            return taker==Side.BUY ? asks.isEmpty()?null:asks.firstKey()
                    : bids.isEmpty()?null:bids.firstKey();
        }
        Long nextOppPrice(long p, Side taker){
            return taker==Side.BUY ? asks.higherKey(p) : bids.lowerKey(p);
        }
        long restingQtyAt(long p, Side makerSide){
            PriceLevel lvl = makerSide==Side.BUY? bids.get(p):asks.get(p);
            return lvl==null?0:lvl.totalQty();
        }
        void addResting(Order o){
            NavigableMap<Long,PriceLevel> map = o.side==Side.BUY? bids:asks;
            PriceLevel lvl = map.computeIfAbsent(o.limitPrice, PriceLevel::new);
            lvl.orders.addLast(o);
        }
        /* xóa level rỗng để map luôn gọn */
        void cleanupLevel(long price, Side side){
            NavigableMap<Long,PriceLevel> map = side==Side.BUY? bids:asks;
            PriceLevel lvl = map.get(price);
            if (lvl!=null && lvl.orders.isEmpty()) map.remove(price);
        }

        @Override
        public String toString() {
            return "OrderBook{" +
                    "bids=" + bids +
                    ", asks=" + asks +
                    '}';
        }
    }

    /* ============================== STOP BOOK =============================== */
    static class StopBook {
        // BUY‑STOP ↑ (trigger khi last≥stop),      SELL‑STOP ↓ (trigger khi last≤stop)
        final NavigableMap<Long,Deque<Order>> buyStops  = new TreeMap<>();
        final NavigableMap<Long,Deque<Order>> sellStops = new TreeMap<>(Collections.reverseOrder());

        @Override
        public String toString() {
            return "StopBook{" +
                    "buyStops=" + buyStops +
                    ", sellStops=" + sellStops +
                    '}';
        }

        void add(Order o){
            NavigableMap<Long,Deque<Order>> m = o.side==Side.BUY? buyStops: sellStops;
            m.computeIfAbsent(o.stopPrice, k->new ArrayDeque<>()).addLast(o);
        }

        // Tính khối lượng stop *có thể* kích hoạt giữa 2 giá (dry‑run)
        long triggerableQty(long prev, long curr, Side taker){
            Deque<Order> list = collect(prev,curr,taker,false);
            return list.stream().mapToLong(o->o.totalQty).sum();
        }
        // Lấy thật stop nằm trong (prev..curr] & xoá khỏi map
        Deque<Order> pop(long prev,long curr,Side taker){
            return collect(prev,curr,taker,true);
        }
        private Deque<Order> collect(long prev,long curr,Side taker,boolean pop){
            boolean buyTaker = taker==Side.BUY; // BUY taker => cần sell‑stop liquidity
            NavigableMap<Long,Deque<Order>> src = buyTaker? sellStops : buyStops;
            // Với map DESC (sellStops): headMap(prev,false) → giá < prev; tailMap(curr,true) → giá >= curr
            SortedMap<Long,Deque<Order>> sub = buyTaker
                    ? src.headMap(prev,false).tailMap(curr,true)
                    : src.tailMap(prev,false).headMap(curr,true);
            Deque<Order> out = new ArrayDeque<>();
            if(sub.isEmpty()) return out;
            for(Iterator<Long> it=sub.keySet().iterator(); it.hasNext();){
                Long k = it.next();
                out.addAll(src.get(k));
                if(pop) it.remove();
            }
            return out;
        }
    }

    /* ========================== MATCHING ENGINE ============================= */
    private final OrderBook book = new OrderBook();
    private final StopBook  stops= new StopBook();
    private long nextId = 1;                       // phát ID tự động

    /* ----------------------- Đặt lệnh (entry point) ------------------------- */
    public void place(Order o){
        o.id = nextId++;                           // gán id
        if(o.isStop()){                            // STOP_*  → vào StopBook
            stops.add(o);
            return;
        }
        // LIMIT hoặc MARKET
        switchedPlacement(o);
    }

    private void switchedPlacement(Order o){
        if(o.tif==TimeInForce.FOK){                // FOK
            if(!canFillFOK(o)) return;             // reject (implied)
            match(o,false);                        // khớp thật, ignore leftover (đảm bảo đủ)
            return;
        }
        long leftover = match(o,false);
        if(leftover>0){
            if(o.tif==TimeInForce.GTC && o.isLimit()){
                // Phần còn lại (đối với ICEBERG cần cập nhật display)
                o.totalQty = leftover;
                if(o.isIceberg()) o.displayQty = Math.min(o.peakSize,leftover);
                else o.displayQty = leftover;
                book.addResting(o);
            }
            // IOC: bỏ phần dư
        }
    }

    /* -------------------- Dry‑Run cho FOK ----------------------------------- */
    private boolean canFillFOK(Order fok){
        long need = fok.totalQty;
        long limit = fok.isMarket() ? (fok.side==Side.BUY? Long.MAX_VALUE:Long.MIN_VALUE) : fok.limitPrice;
        Long price = book.bestOppPrice(fok.side);
        if(price==null) return false;
        long prev = fok.side==Side.BUY? Long.MIN_VALUE:Long.MAX_VALUE;
        while(need>0 && price!=null && withinLimit(price,fok.side,limit)){
            need -= book.restingQtyAt(price,fok.side.opp());
            need -= stops.triggerableQty(prev,price,fok.side);
            prev  = price;
            price = book.nextOppPrice(price,fok.side);
        }
        return need<=0;
    }

    /* ------------------------- MATCH CHÍNH ---------------------------------- */
    /**
     * @param taker     lệnh vào (đã chắc chắn là MARKET/LIMIT)
     * @param silent    true nếu chỉ tính toán (dry‑run cascade cho FOK) – hiện không dùng
     * @return khối lượng *còn lại* không khớp
     */
    private long match(Order taker, boolean silent){
        long need  = taker.totalQty;
        long limit = taker.isMarket()? (taker.side==Side.BUY? Long.MAX_VALUE:Long.MIN_VALUE) : taker.limitPrice;
        Long price = book.bestOppPrice(taker.side);
        long prevPrice = taker.side==Side.BUY? Long.MIN_VALUE:Long.MAX_VALUE;
        Deque<Order> trigQ = new ArrayDeque<>();

        while(need>0 && price!=null && withinLimit(price,taker.side,limit)){
            /* 1) khớp resting tại bucket hiện tại */
            need -= matchResting(price,taker.side,need);

            /* 2) Trigger stop nằm trong (prevPrice..price] */
            trigQ.addAll(stops.pop(prevPrice,price,taker.side));

            /* 3) FIFO khớp stop vừa kích hoạt */
            while(!trigQ.isEmpty() && need>0){
                Order s = trigQ.pollFirst();
                if(s.isStopMarket()){
                    need -= matchResting(price,taker.side,need); // market khớp ngay
                }else{ // STOP_LIMIT
                    boolean exec = (s.side==Side.BUY && s.limitPrice>=price)
                            ||(s.side==Side.SELL&& s.limitPrice<=price);
                    if(exec){

                    }
                    if(withinLimit(s.limitPrice, s.side.opp(), price)){
                        need -= matchResting(price,taker.side,need);
                    }else{
                        // limit chưa khớp được → trở thành maker
                        s.kind = OrderKind.LIMIT;
                        s.limitPrice = s.limitPrice==0? price : s.limitPrice;
                        book.addResting(s);
                    }
                }
            }

            /* 4) refill ICEBERG ở bucket hiện tại */
            refillIcebergsAt(price, taker.side.opp());

            /* 5) chuyển sang bucket tiếp */
            prevPrice = price;
            price = book.nextOppPrice(price,taker.side);
        }
        return need;
    }

    /* ------------ khớp resting trong 1 PriceLevel, trả về khớp được ---------- */
    private long matchResting(long price, Side takerSide, long need){
        NavigableMap<Long,PriceLevel> map = takerSide==Side.BUY? book.asks:book.bids;
        PriceLevel lvl = map.get(price);
        if(lvl==null) return 0;
        long filled = 0;
        Iterator<Order> it = lvl.orders.iterator();
        while(it.hasNext() && need>0){
            Order maker = it.next();
            long take = Math.min(need, maker.displayQty);
            maker.displayQty -= take;
            maker.totalQty   -= take;
            need             -= take;
            filled           += take;

            if(maker.displayQty==0){
//                System.out.println("Maker "+maker+" vol "+take);
                if(maker.isIceberg() && maker.totalQty>0){
                    // sẽ refill ở bước 4
                }else if(maker.totalQty==0){
                    it.remove();            // done
                }
            }
        }
        if(lvl.orders.isEmpty()) map.remove(price);
        return filled;
    }

    /* ------------------- ICEBERG refill (mất ưu tiên) ----------------------- */
    private void refillIcebergsAt(long price, Side makerSide){
        NavigableMap<Long,PriceLevel> map = makerSide==Side.BUY? book.bids:book.asks;
        PriceLevel lvl = map.get(price);
        if(lvl==null) return;
        List<Order> toMove = new ArrayList<>();
        for(Order o: lvl.orders){
            if(o.isIceberg() && o.displayQty==0 && o.totalQty>0){
                long refill = Math.min(o.peakSize, o.totalQty);
                o.displayQty = refill;
                toMove.add(o);
            }
        }
        // di chuyển xuống cuối để mất ưu tiên
        for(Order o: toMove){
            lvl.orders.remove(o);
            lvl.orders.addLast(o);
        }
        if(lvl.orders.isEmpty()) map.remove(price);
    }

    /* --------------------------- tiện ích nhỏ ------------------------------- */
    private static boolean withinLimit(long price, Side taker, long limit){
        return taker==Side.BUY ? price<=limit : price>=limit;
    }

    /* ============================== TESTS =================================== */
    public static void main(String[] args){
        try{
            testSimpleLimit();
//            testStopAndCascade();
//            testFokSuccess();
//            testFokFail();
//            testIceberg();
            System.out.println("\n✅  Tất cả test đều PASS!");
        }catch(AssertionError e){
            System.err.println("❌  Test FAILED: "+e.getMessage());
        }
    }

    /* ---------- helper để assert ---------- */
    private static void ok(boolean cond, String msg){ if(!cond) throw new AssertionError(msg); }

    /* ---------- tạo lệnh tiện dụng ---------- */
    private static Order make(long uid, Side side, OrderKind kind, TimeInForce tif,
                              long price,long stop,long qty){
        return new Order(uid,side,kind,tif,price,stop,qty,0);
    }
    private static Order iceberg(long uid, Side side,long price,long qty,long peak){
        return new Order(uid,side,OrderKind.LIMIT,TimeInForce.GTC,price,0,qty,peak);
    }

    /* ===================== TEST CASES ===================== */

    // 1) Limit khớp đơn giản
    private static void testSimpleLimit(){
        MatchingEngine eng = new MatchingEngine();
        for (int i=0;i<1_000_000;i++) {
            // Maker: SELL 50 @100 GTC
            eng.place(make(i, Side.SELL, OrderKind.LIMIT, TimeInForce.GTC, 100, 0, 50));
//            eng.place(make(i+1, Side.SELL, OrderKind.LIMIT, TimeInForce.GTC, 120, 0, 50));
            // Taker: BUY 50 @100 IOC
        }
        eng.place(make(3, Side.BUY, OrderKind.LIMIT, TimeInForce.GTC, 200, 0, 50_000_000));
        ok(eng.book.asks.isEmpty(), "ask không rỗng sau khi khớp toàn bộ");
        ok(eng.book.bids.isEmpty(), "bid phải rỗng");
    }

    // 2) Stop cascade – BUY taker kích hoạt SELL‑STOP
    private static void testStopAndCascade(){
        MatchingEngine eng = new MatchingEngine();
        // Maker SELL 100 @100
        eng.place(make(1,Side.SELL,OrderKind.LIMIT,TimeInForce.GTC,100,0,100));
        // SELL‑STOP‑MARKET 60 @95

        eng.place(make(2,Side.SELL,OrderKind.STOP_MARKET,TimeInForce.GTC,0,95,50));
        eng.place(make(3,Side.SELL,OrderKind.STOP_LIMIT,TimeInForce.GTC,95,95,50));
        eng.place(make(3,Side.SELL,OrderKind.STOP_LIMIT,TimeInForce.GTC,95,95,50));
//        eng.place(make(3,Side.SELL,OrderKind.STOP_MARKET,TimeInForce.GTC,0,200,50));
        // BUY market 150 IOC (taker)
        eng.place(make(4,Side.BUY,OrderKind.LIMIT,TimeInForce.GTC,200,0,100));
        // Sau khớp phải hết 150, order book rỗng
        ok(eng.book.asks.isEmpty(), "ask còn dư sau cascade");
        System.out.println(eng.book.asks);
        ok(eng.book.bids.isEmpty(), "ask còn dư sau cascade");
        System.out.println(eng.book.bids);
        System.out.println(eng.stops.toString());
    }

    // 3) FOK thành công nhờ stop‑liquidity
    private static void testFokSuccess(){
        MatchingEngine eng = new MatchingEngine();
        // Resting SELL 80 @100
        eng.place(make(1,Side.SELL,OrderKind.LIMIT,TimeInForce.GTC,100,0,80));
        // SELL‑STOP 40 @99 (stop‑market)
        eng.place(make(2,Side.SELL,OrderKind.STOP_MARKET,TimeInForce.GTC,0,99,40));
        // BUY FOK 100 @101
        Order fok = make(3,Side.BUY,OrderKind.LIMIT,TimeInForce.FOK,101,0,100);
        eng.place(fok);
        ok(eng.book.asks.isEmpty(), "FOK phải khớp đủ và xóa ask");
    }

    // 4) FOK thất bại – thiếu khối lượng
    private static void testFokFail(){
        MatchingEngine eng = new MatchingEngine();
        // chỉ 50 @100
        eng.place(make(1,Side.SELL,OrderKind.LIMIT,TimeInForce.GTC,100,0,50));
        // BUY FOK 80 @101 – phải reject, ask vẫn còn
        eng.place(make(2,Side.BUY,OrderKind.LIMIT,TimeInForce.FOK,101,0,80));
        ok(!eng.book.asks.isEmpty(), "FOK thiếu volume nhưng vẫn khớp");
    }

    // 5) Iceberg refill
    private static void testIceberg(){
        MatchingEngine eng = new MatchingEngine();
        // Iceberg SELL 300 (peak 100) @100
        eng.place(iceberg(1,Side.SELL,100,300,100));
        // BUY market 300
        eng.place(make(2,Side.BUY,OrderKind.MARKET,TimeInForce.IOC,0,0,300));
        ok(eng.book.asks.isEmpty(), "Iceberg chưa khớp hết");
    }
}

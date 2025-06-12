package org.example;

import java.util.*;

/**
 * Matching engine – BFS with travelling LIMIT logic (verbose).
 *
 * Fixes:
 *  • Balanced braces and correct placement of helper methods.
 *  • popTriggered now properly buckets by limitPrice.
 *  • Clearer logging strings without breaking Unicode compile safety.
 */

/* ======================  Order  ====================== */
class Order {
    long    id;         // surrogate timestamp
    boolean buy;        // true = BUY
    long    limit;      // Long.MAX_VALUE ⇒ market
    double  qty;        // remaining qty
    boolean stop;       // stop placeholder
    long    trigger;    // trigger price

    Order(long id, boolean buy, long price, double qty) {
        this.id=id; this.buy=buy; this.limit=price; this.qty=qty;
    }
    Order(long id, boolean buy, long trigger, long limit, double qty) {
        this(id,buy,limit,qty); this.stop=true; this.trigger=trigger;
    }
    boolean isMarket(){return limit==Long.MAX_VALUE;}
    public String toString(){return (stop?"Stop":"Ord")+"{"+id+","+(buy?"B":"S")+",p="+(isMarket()?"MKT":limit)+",q="+qty+(stop?",@"+trigger:"")+'}';}
}

/* ====================  OrderBook  ==================== */
class OrderBook {
    /* active books */
    final TreeMap<Long,List<Order>> bid = new TreeMap<>(Collections.reverseOrder());
    final TreeMap<Long,List<Order>> ask = new TreeMap<>();

    /* stop books */
    private final TreeMap<Long,List<Order>> sbBuyGE  = new TreeMap<>();
    private final TreeMap<Long,List<Order>> sbBuyLE  = new TreeMap<>();
    private final TreeMap<Long,List<Order>> sbSellGE = new TreeMap<>();
    private final TreeMap<Long,List<Order>> sbSellLE = new TreeMap<>();

    private List<Order> newBucket(){ return new LinkedList<>(); }

    /* ----- adders ----- */
    void addLimit(Order o){
        System.out.println("  [add] LIMIT  "+o);
        (o.buy?bid:ask).computeIfAbsent(o.limit,k->new ArrayList<>()).add(o);
    }
    void addStop(Order s,long last){
        System.out.println("  [add] STOP   "+s+" (last="+last+")");
        TreeMap<Long,List<Order>> map = s.buy ? (s.trigger>=last?sbBuyGE:sbBuyLE)
                : (s.trigger>=last?sbSellGE:sbSellLE);
        map.computeIfAbsent(s.trigger,k->newBucket()).add(s);
    }

    /* ----- trigger ----- */
    TreeMap<Long,List<Order>> popTriggered(long px){
        TreeMap<Long,List<Order>> res = new TreeMap<>();
        collect(sbBuyGE ,true ,px,res);
        collect(sbBuyLE ,false,px,res);
        collect(sbSellGE,true ,px,res);
        collect(sbSellLE,false,px,res);
        if(!res.isEmpty())
            System.out.println("  [trigger] price="+px+" buckets="+res.keySet());
        return res;
    }
    private void collect(TreeMap<Long,List<Order>> src, boolean ge, long px, TreeMap<Long,List<Order>> dst){
        NavigableMap<Long,List<Order>> slice = ge ? src.headMap(px,true) : src.tailMap(px,true);
        for(List<Order> bucket : slice.values()){
            for(Order s: bucket){ dst.computeIfAbsent(s.limit,k->newBucket()).add(s); }
        }
        dst.values().forEach(l->l.sort(Comparator.comparingLong(o->o.id)));
        slice.clear();
    }

    /* ----- printers ----- */
    void printLimit(){
        System.out.println("=== LIMIT BOOKS ===");
        System.out.println(" BID:"); bid.forEach((p,l)->l.forEach(o->System.out.println("  "+o)));
        System.out.println(" ASK:"); ask.forEach((p,l)->l.forEach(o->System.out.println("  "+o)));
    }
    void printStop(){
        System.out.println("=== STOP BOOKS ===");
        Map.of("BUY ≥",sbBuyGE,"BUY ≤",sbBuyLE,"SELL ≥",sbSellGE,"SELL ≤",sbSellLE)
                .forEach((name,tm)->{ System.out.println(" "+name+":");
                    tm.forEach((p,l)->l.forEach(o->System.out.println("  "+o)));});
    }
}

/* ================= MatchingEngine ================= */
class MatchingEngine {
    private final OrderBook ob = new OrderBook();
    private final Deque<Order> queue = new ArrayDeque<>();
    private long lastPrice = 0;

    /* public entry */
    void submit(Order o){
        System.out.println(" >>> SUBMIT "+o);
        if(o.stop) ob.addStop(o,lastPrice); else queue.addLast(o);
        loop();
    }

    /* main processing loop */
    private void loop(){
        while(!queue.isEmpty()){
            Order inc = queue.pollFirst();
            System.out.println(" --- PROCESS "+inc);
            boolean deferred = sweepLayers(inc);
            if(!deferred && inc.qty>0){
                if(inc.isMarket()) System.out.printf("  [warn] Market leftover %.2f cancelled%n",inc.qty);
                else ob.addLimit(inc);
            }
            ob.printLimit(); ob.printStop();
            System.out.println("--------------------------------------------");
        }
    }

    /* returns true if incoming re‑queued for another layer */
    private boolean sweepLayers(Order inc){
        while(inc.qty>0){
            Map.Entry<Long,List<Order>> opp = inc.buy? ob.ask.firstEntry(): ob.bid.firstEntry();
            if(opp==null){System.out.println("  [info] No liquidity"); return false;}
            if(!cross(inc, opp.getKey())){System.out.println("  [cap] limit reached @"+opp.getKey()); return false;}
            hitBucket(inc, opp.getValue(), opp.getKey());
            if(opp.getValue().isEmpty()) (inc.buy?ob.ask:ob.bid).remove(opp.getKey());
            if(handleStopsLayer(inc)) return true;  // incoming pushed for next layer
        }
        return false; // fully filled
    }

    private boolean cross(Order in,long px){return in.isMarket()||(in.buy?in.limit>=px:in.limit<=px);}

    private void hitBucket(Order taker,List<Order> makers,long level){
        Iterator<Order> it = makers.iterator();
        while(it.hasNext() && taker.qty>0){
            Order mk = it.next();
            double vol = Math.min(taker.qty, mk.qty);
            taker.qty -= vol; mk.qty -= vol;
            lastPrice = mk.isMarket()? level : mk.limit;
            System.out.printf("  trade %.2f @ %d  (t%d vs m%d)%n",vol,lastPrice,taker.id,mk.id);
            if(mk.qty==0) it.remove();
        }
    }

    /* handle all stops for current price layer; return true if incoming re‑queued */
    private boolean handleStopsLayer(Order inc){
        boolean requeueIncoming = false;
        boolean priceChangedInLayer;
        do {
            priceChangedInLayer = false;
            TreeMap<Long,List<Order>> buckets = ob.popTriggered(lastPrice);
            if(buckets.isEmpty()) break;
            NavigableMap<Long,List<Order>> iter = inc.buy? buckets.descendingMap() : buckets;
            boolean flag = false;
            for(var entry : iter.entrySet()){
                flag = false;
                for(Order stop : entry.getValue()){
                    boolean cross = crossWithInc(inc, stop);
                    if(cross && inc.qty>0){
                        flag = true;
                        long oldPx = lastPrice;
                        directOnce(inc, stop);
                        if(lastPrice != oldPx) priceChangedInLayer = true; // we will pull new triggers in next loop cycle
                    }
                    if(stop.qty>0){ if(cross) queue.addLast(stop); else ob.addLimit(stop);}

                }
                if(flag){
                    TreeMap<Long,List<Order>> buckets2 = ob.popTriggered(lastPrice);
                    NavigableMap<Long,List<Order>> iter2 = inc.buy? buckets2.descendingMap() : buckets;
                    for(var entry2 : iter2.entrySet()) {
                        for (Order stop : entry2.getValue()) {
                            queue.addLast(stop);
                        }
                    }
                }
            }
        } while(priceChangedInLayer);

        if(inc.qty>0){ queue.addLast(inc); requeueIncoming = true; }
        return requeueIncoming;
    }

    /* execute a single direct match (no recursive triggers inside) */
    private void directOnce(Order inc, Order trg){
        double vol = Math.min(inc.qty, trg.qty);
        inc.qty -= vol; trg.qty -= vol;
        lastPrice = trg.limit;
        System.out.printf("      direct %.2f @ %d%n",vol,lastPrice);
    }

    private boolean crossWithInc(Order inc, Order trg){return trg.isMarket()||(inc.buy?inc.limit>=trg.limit:inc.limit<=trg.limit);} }

/* ========================= Demo ========================= */
public class MatchingEngineDemo {
    public static void main(String[] args) {
        MatchingEngine eng = new MatchingEngine();
        eng.submit(new Order(1,true ,80,100));
        eng.submit(new Order(2,false,100,100));
        eng.submit(new Order(3,false,100,90,50));
        eng.submit(new Order(6,false,90,90,50));
        eng.submit(new Order(4,false,100,85,100));
        eng.submit(new Order(5,true ,200,200));
    }
}

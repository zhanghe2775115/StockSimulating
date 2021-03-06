package com.zhanghe.stockSimulating.Institution.exchangeCentor;


import com.zhanghe.stockSimulating.Institution.exchangeCentor.OrderQueue.OrderProcessThread;
import com.zhanghe.stockSimulating.Institution.exchangeCentor.OrderQueue.OrderProcessThreadPoolManager;
import com.zhanghe.stockSimulating.Institution.exchangeCentor.OrderQueue.OrderQueueManager;
import com.zhanghe.stockSimulating.Institution.exchangeCentor.OrderQueue.SortedOrderQueue;

import com.zhanghe.stockSimulating.Util.Enum.WorkState;
import com.zhanghe.stockSimulating.facade.bean.Order;
import com.zhanghe.stockSimulating.facade.bean.Stock;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Drake on 2018/3/11.
 */
public class StockExchangeCentor {
    /* concurrent thread pool setting */
    private final static int CORE_POOL_SIZE = 2;
    // 线程池维护线程的最大数量
    private final static int MAX_POOL_SIZE = 10;
    // 线程池维护线程所允许的空闲时间
    private final static int KEEP_ALIVE_TIME = 0;
    // 线程池所使用的缓冲队列大小
    private final static int WORK_QUEUE_SIZE = 50;
    // 消息缓冲队列
    private Queue<Order> cacheQueue = null;//= new ConcurrentLinkedDeque<Order>();
    private Queue<Order> queueImplA = new ConcurrentLinkedDeque<Order>();
    private Queue<Order> queueImplB = new ConcurrentLinkedDeque<Order>();
    private String exChangeCentorName;
    private List<Stock> stocks;
    private static StockExchangeCentor stockExchangeCentor;
    private Queue<Order> producerQueue;
    private Queue<Order> consumerQueue;
    private WorkState inProcessState;
    private WorkState workState;
    private InProcessThread inProcessThread;
    private static OrderProcessThreadPoolManager orderProcessThreadPoolManager;

    private SortedOrderQueue[] waitedOrders;
    //private SortedOrderQueue waitedProcessingSoldOrder = new SortedOrderQueue();
   // private SortedOrderQueue waitedProcessingBuyingOrder = new SortedOrderQueue();


    final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new ArrayBlockingQueue(WORK_QUEUE_SIZE));

    public StockExchangeCentor(String exChangeCentorName) {
        producerQueue = queueImplA;//new LinkedList<Order>();
        consumerQueue = queueImplB;
        cacheQueue = new ConcurrentLinkedDeque<Order>();
        inProcessThread = new InProcessThread();
        this.exChangeCentorName = exChangeCentorName;
        //waited queue init
        waitedOrders = OrderQueueManager.getInstance().getWaitedOrdersQueue();
        orderProcessThreadPoolManager = new OrderProcessThreadPoolManager();

    }

    public boolean pushOrder(Order order) {
        StringBuilder sb = new StringBuilder(order.getOrderID());
        sb.append(System.currentTimeMillis());
        order.setOrderID(sb.toString());
        producerQueue.add(order);
        return true;
    }

    private class InProcessThread extends Thread {
        public InProcessThread() {
            System.out.println("InProcessThread construct");
        }
        public void run() {
            System.out.print("InProcessThread start");
            try {
                while (inProcessState == WorkState.Working) {
                    if (!producerQueue.isEmpty()) {
                        while (!producerQueue.isEmpty()) {
                            synchronized (producerQueue) {
                                Queue<Order> temp = producerQueue;
                                producerQueue = consumerQueue;
                                consumerQueue = temp;
                            }
                            OrderProcessThread orderProcessThread;
                            while (!consumerQueue.isEmpty()) {
                                Order order;
                                order = consumerQueue.poll();
                                waitedOrders[order.getOrderType().getId()].pushOrder(order);
//                              threadPool.execute(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        Order order;
//                                        order = consumerQueue.poll();
//
//                                        try {
//                                            waitedOrders[order.getOrderType().getId()].pushOrder(order);
//                                        }catch (Exception e ){
//                                            while (true){}
//                                        }
//
//                                    }
//                                });
                            }
                            sleep(500);
                        }
                    }
                }
            } catch (Exception e) {
            e.printStackTrace();
            }
        }
    }

    public static void start() {
        StockExchangeCentor stockExchangeCentor = getInstance();
        stockExchangeCentor.setWorkState(WorkState.Working);
        stockExchangeCentor.setInProcessState(WorkState.Working);
        stockExchangeCentor.inProcessThread.start();
        orderProcessThreadPoolManager.startProcess();
    }

    public static StockExchangeCentor getInstance() {
        if (stockExchangeCentor == null) {
            synchronized (StockExchangeCentor.class) {
                stockExchangeCentor = new StockExchangeCentor("NewYork");
            }
        }
        return stockExchangeCentor;
    }

    public Stock getStock(int id) {
        for (Stock s : stocks
        ) {
            if (s.getId() == id)
                return s;
        }
        return null;
    }

    public String getExChangeCentorName() {
        return exChangeCentorName;
    }

    public void setExChangeCentorName(String exChangeCentorName) {
        this.exChangeCentorName = exChangeCentorName;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }
    public WorkState getWorkState() {
        return workState;
    }

    public void setWorkState(WorkState workState) {
        this.workState = workState;
    }

    public WorkState getInProcessState() {
        return inProcessState;
    }

    public void setInProcessState(WorkState inProcessState) {
        this.inProcessState = inProcessState;
    }

}

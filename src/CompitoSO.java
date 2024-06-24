import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

class ArrayGenerator{
    int k;
    int TG;
    int a = 1;
    int nGen = 0;
    Semaphore mutex = new Semaphore(1);
    public ArrayGenerator(int k, int TG) {
        this.k = k;
        this.TG = TG;
    }
    public int[] getArray() throws InterruptedException {
        mutex.acquire();
        int[] array = new int[k];
        for (int i = 0; i < k; i++)
            array[i] = i + a;
        Thread.sleep(TG);
        nGen++;
        a++;
        mutex.release();
        return array;
    }
}

class Result{
    int result;
    int[] array;

    public Result(int r, int[] arr) {
        result = r;
        array = arr;
    }
}

class ProcessorThread extends Thread{
    ArrayGenerator generator;
    ResourceManager resourceManager;
    int T;
    Queue queue;
    int nProc = 0;
    public ProcessorThread(ArrayGenerator generator,ResourceManager resourceManager,int T, Queue queue){
        this.generator=generator;
        this.resourceManager=resourceManager;
        this.T=T;
        this.queue=queue;
    }

    @Override
    public void run(){
        try {
            while(true){
                int[] array = generator.getArray();
                int r = 1+(int)(Math.random()*2);
                int sum=0;
                resourceManager.acquireResource(r);
                try {
                    for (int j : array)
                        sum += j;
                    sleep(T);
                    queue.putResult(sum, array);
                    nProc++;
                }finally {
                    resourceManager.releaseResource(r);
                }
            }
        } catch (InterruptedException e) {

        }
    }
}

class Queue{
    ArrayList<Result> results = new ArrayList<>();
    Semaphore mutex = new Semaphore(1);
    Semaphore piene = new Semaphore(0);
    Semaphore vuote;
    public Queue(int L){vuote=new Semaphore(L);}
    public void putResult(int s,int[] a) throws InterruptedException {
        vuote.acquire();
        mutex.acquire();
        Result r = new Result(s, a);
        results.add(r);
        mutex.release();
        piene.release();
    }

    public Result[] getResult() throws InterruptedException{
        piene.acquire(2);
        mutex.acquire();
        Result[] r = new Result[2];
        r[0]=results.removeFirst();
        r[1]=results.removeFirst();
        mutex.release();
        vuote.release(2);
        return r;
    }
}

class ResourceManager{
    Semaphore resources;
    public ResourceManager(int NA){
        resources=new Semaphore(NA);
    }

    public void acquireResource(int n) throws InterruptedException{
        resources.acquire(n);
    }
    public void releaseResource(int n) throws InterruptedException{
        resources.release(n);
    }
}
class OutputThread extends Thread{
    Queue queue;
    int nStamp = 0;
    public OutputThread(Queue queue){
        this.queue=queue;
    }

    @Override
    public void run(){
        try{
            while(true){
                Result[] r = queue.getResult();
                System.out.println("Array: "+Arrays.toString(r[0].array)+" Result: "+r[0].result);
                System.out.println("Array: "+Arrays.toString(r[1].array)+" Result: "+r[1].result);
                nStamp+=2;
            }
        }catch(InterruptedException e){

        }
    }
}

public class CompitoSO{
    public static void main(String[] args) throws InterruptedException {
        int NA=8;
        int L=10;
        int TG = 100;
        int T = 1000;
        int k=5;
        int N=4;

        ResourceManager rm = new ResourceManager(NA);
        Queue q = new Queue(L);
        ArrayGenerator ag = new ArrayGenerator(k,T);

        ProcessorThread[] pt = new ProcessorThread[N];
        for(int i=0;i<pt.length;i++){
            pt[i]=new ProcessorThread(ag,rm,TG,q);
            pt[i].setName("PT"+i);
            pt[i].start();
        }

        OutputThread[] ot = new OutputThread[2];
        for (int i=0;i<ot.length;i++){
            ot[i]=new OutputThread(q);
            ot[i].setName("OT"+i);
            ot[i].start();
        }

        Thread.sleep(10000);
        System.out.println();

        for(ProcessorThread p : pt)
            p.interrupt();

        for(OutputThread o : ot)
            o.interrupt();

        for(ProcessorThread p : pt){
            p.join();
            System.out.println(p.getName()+" has processed "+p.nProc+" arrays");
        }

        System.out.println();

        for(OutputThread o : ot){
            o.join();
            System.out.println(o.getName()+" has printed "+o.nStamp+" arrays");
        }

        System.out.println();
        System.out.println("Generated arrays: "+ag.nGen);
        System.out.println("Available resources: "+rm.resources.availablePermits());
        System.out.println("Remaining array in queue: "+q.results.size());
    }
}





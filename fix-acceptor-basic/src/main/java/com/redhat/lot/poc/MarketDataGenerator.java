package com.redhat.lot.poc;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Date;

import quickfix.DoubleField;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.UtcTimeStampField;
import quickfix.field.TransactTime;

public class MarketDataGenerator implements Runnable {


	//private final static String msg = "8=FIX.4.49=12835=D34=449=STUN52=20210715-21:06:54.41656=EXEC11=162638321441821=138=340=154=155=VALE59=060=20210715-21:06:54.41610=015";
	private static String msg = "8=FIX.4.49=12835=D34=449=STUN52=20210715-21:06:54.41656=EXEC11=162638321441821=138=340=154=155=VALE59=060=changedate10=015";
	
	private String fixDatePattern = "YYYYMMdd-HH:mm:ss.SSS";
	private static SimpleDateFormat simpleDateFormat;
	private boolean play = true;
	private int quantity = 100;

	private long duration;
	private int chunks=1;
	private long initPerSecondTime;
	private long currenttime;
	private long totalMessagesGenerated;
	private int errors = 0;
	private int time_left=0;
	private String msg2;
	private int i;
	
	
	private static MarketDataGenerator instance;
	

	//end time execution, since initTime (inittime + (duration in milliseconds))
	private long endTime;



	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setPlay(boolean play) {
		this.play = play;
	}
	
	public int getChunks() {
		return chunks;
	}

	public void setChunks(int chunks) {
		this.chunks = chunks;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}



	public MarketDataGenerator(int quantity, int duration) {

		this.quantity = quantity;
		this.duration = duration;

		
		this.simpleDateFormat = new SimpleDateFormat(fixDatePattern);
		
	}
	
	public static MarketDataGenerator getInstance() {
		if(instance == null)
			instance = new MarketDataGenerator();
		return instance;
	}
	
	public MarketDataGenerator() {
	}

	@Override
	public void run() {
		

		totalMessagesGenerated = 0;
		
		// End Time = Time until the thread will be executed
		endTime = System.currentTimeMillis() + duration;
		currenttime = System.currentTimeMillis();
		
		//System.out.println(">>> Arrancando... Generando " + quantity+ "] mensajes cada "+interval+" ms Durante "+duration+" ms, NanoNow: "+currenttime+", endTime: "+endTime);
		
		
		System.out.println(">>> Arrancando... Generando " + quantity+ " mensajes cada 1 ms Durante "+duration+" ms, NanoNow: "+currenttime+", endTime: "+endTime);
	
		
		simpleDateFormat = new SimpleDateFormat(fixDatePattern);
		
		try {
			while (play) {
				generateMarketData2();
				currenttime = System.currentTimeMillis();
				if(currenttime>=endTime) {
					//System.out.println((">>> Time is up! currentTime: "+currenttime+" >= "+endTime));
					stop();
				}
			}
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}

	}
	
	public void generateMarketData2() throws InterruptedException {
		
		msg2 = MarketDataGenerator.generateStringMessage(); //all with the same timestamp in each cycle
		i = 0;
		initPerSecondTime = System.nanoTime();
		for (i = 0; i < (quantity); i++) {
			
			CircularList.getInstance().insert(msg2);
		}
		totalMessagesGenerated = totalMessagesGenerated + i;
		time_left = 1000000 - (int) (System.nanoTime() - initPerSecondTime) ;
		if (time_left < 0) {
			//means that took more than 1 milisecond
			errors++;
		} else {
			Thread.currentThread().sleep(0,time_left);
		}
		
	}



	public static Message generateMessage(){
		Message fixMessage = new Message();

		try {
			fixMessage.fromString(msg, null, false);
			//fixMessage.setField(new DoubleField(60, System.currentTimeMillis() ));
			
			fixMessage.setField(new TransactTime( java.time.LocalDateTime.now() ));
			
		} catch (InvalidMessage e) {
			e.printStackTrace();
		}

		return fixMessage;
	}

	public static String generateStringMessage(){
		String strFixMessage = msg;//"8=FIX.4.49=12835=D34=449=STUN52=20210901-00:06:54.41656=EXEC11=162638321441821=138=340=154=155=VALE59=0";

		try {
			
			strFixMessage = strFixMessage.replace("changedate", simpleDateFormat.format(new Date()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return strFixMessage;
	}
	

	
	public void stop() {
		
		System.out.println((">>> Time is up ("+duration+" ms)! Stopping thread..."));
		System.out.println((">>> Total Messages Generated... ("+totalMessagesGenerated+")"));
		System.out.println((">>> Time errors... ("+errors+")"));
		
		Metrics.getInstance().logMetrics();
		
		play = false;

	}
	

}

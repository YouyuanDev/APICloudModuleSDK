package com.apiclound.zebraprinter;

 
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionBuilder;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.widget.Toast;
import android.database.Cursor;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
public class ZebraAndroidPrint extends UZModule implements DiscoveryHandler{  
    public static List<String>printerAddress=new ArrayList<String>();
    public static JSONArray printers=new JSONArray();
	private static Connection connection=null;
	private static ZebraPrinter printer=null;
	private static Set<BluetoothDevice>devices=null;
	final Handler handler=new Handler();	
	Runnable runnable=null;
	private static String current_address="";
	public ZebraAndroidPrint(UZWebView webView){
		super(webView);
	}
	//搜索附近打印机
	public void jsmethod_findPrinters(final UZModuleContext moduleContext){
        //TODO
		final JSONObject ret = new JSONObject();
		 Thread t=new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				 try{
					    printerAddress.clear();
					    printers=null;printers=new JSONArray();
						BluetoothDiscoverer.findPrinters(moduleContext.getContext(), ZebraAndroidPrint.this);
						BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
						if(!adapter.isEnabled()){
							adapter.enable();
						}
						//设置绑定过的蓝牙设备
						devices=adapter.getBondedDevices();
						if(devices==null){
							new HashSet<BluetoothDevice>();
						}else{
							Iterator<BluetoothDevice> it = devices.iterator();  
							while (it.hasNext()) {  
							  BluetoothDevice device = it.next();  
							  if(!printerAddress.contains(device.getAddress())){
								  printerAddress.add(device.getAddress());
								  JSONObject obj=new JSONObject();
								  obj.put("address", device.getAddress());
								  obj.put("name", device.getName());
								  printers.put(obj);
							  }
							} 				 
						}			 
					    runnable=new Runnable(){
							@Override
							public void run() {
								try {
									ret.put("result", true);					 
									ret.put("msg",printers);
									moduleContext.success(ret, false);
									handler.postDelayed(this,5000);
								} catch (JSONException e) {										 
								}	    		
							}				
						};
						handler.post(runnable); 
					}catch(Exception ex){			 
					}
			}		 
		 });
        t.start();
    }
	//配对或连接打印机
	public void jsmethod_connectPrinter(final UZModuleContext moduleContext){
        //TODO		 
		Thread t=new Thread(new Runnable(){
			@Override
			public void run() {			
				// TODO Auto-generated method stub
				JSONObject ret = new JSONObject();
				try{
					handler.removeCallbacks(runnable);//关闭5秒钟向前台返回一次runnable 
					String macAddress=moduleContext.optString("printerAddr");				 			
					//通过mac地址连接蓝牙打印机
					if(macAddress!=null&&!"".equals(macAddress)){
						String finalConnectionString="BT:"+macAddress;
						//连接打印机,如果当前连接为空 或者 传过来的mac地址和上次不一样，则重新建立连接					 
						if(connection==null||!current_address.equals(macAddress)){
					      connection = ConnectionBuilder.build(finalConnectionString);
					      if(connection!=null){
					    	  connection.open();
					    	  //获取连接的打印机
							  if(printer==null||!current_address.equals(macAddress)){
									printer = ZebraPrinterFactory.getInstance(connection);
									if(printer!=null){
										ret.put("result", true);
										ret.put("msg","连接成功!");
									}else{
										 ret.put("result", false);
										 ret.put("msg","连接打印机失败!");
									}
							  }else{
								  ret.put("result", true);
								  ret.put("msg","连接成功!");
							  }
					      }else{
					    	  ret.put("result", false);
							  ret.put("msg","连接打印机失败!");
					      }
						}else{
							ret.put("result", true);
							ret.put("msg","连接成功!");
						}									    
					}else{
						ret.put("result", false);
						ret.put("msg","连接错误,未找到Mac地址!");
					}			
				}catch(Exception ex){				 
					try{
						ret.put("result", false);
						ret.put("msg","无法连接该打印机!");
					}catch(Exception exx){						
					}			 
				}finally{					 
					moduleContext.success(ret, true);							 		 
				}
			}
		});	
		t.start();
    } 
	//获取打印机状态
	public void jsmethod_getPrinterStatus(final UZModuleContext moduleContext){
        //TODO
		new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				JSONObject ret = new JSONObject();
				try{
					String address=moduleContext.optString("printerAddr");
					if(address!=null&&!"".equals(address)){
						if(printer!=null){
							 PrinterStatus printerStatus =printer.getCurrentStatus();							 
							 ret.put("result",true);
							 if (printerStatus.isReadyToPrint) {
								 ret.put("msg","ReadyToPrint");
				             } else if (printerStatus.isPaused) {
				            	 ret.put("msg","Paused");
				             } else if (printerStatus.isHeadOpen) {
				            	 ret.put("msg","HeadOpen");
				             } else if (printerStatus.isHeadTooHot) {
				            	 ret.put("msg","HeadTooHot");
				             } else if (printerStatus.isPaperOut) {
				            	 ret.put("msg","PaperOut");
				             } else {
				            	 ret.put("msg","Other");
				             }
						}else{
							ret.put("result",true);
							ret.put("msg","printer not connected!");
						}
					}else{
						ret.put("result", false);
						ret.put("msg","Mac address not found!");
					}	
				}catch(Exception ex){
					try{
						ret.put("result", false);
						ret.put("msg","6");
					}catch(Exception exx){
						
					}				
				}finally{
					moduleContext.success(ret, true);
				}
			}
			
		}).start();	
    }
	//打印图片
	public void jsmethod_printImage(final UZModuleContext moduleContext){
        //TODO
		new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				JSONObject ret = new JSONObject();
				try{
					String imageUrl=moduleContext.optString("imageUrl");//imageUrl为widget://或fs://
					//X -水平起点位置的点,y在点上垂直起始位置,width-打印图像的期望宽度。
					//传递小于1的值将保留原始宽度,height-期望高度。传递小于1的值将保持原始高度。copies 表示打印的次数
					int x=0,y=0,width=-1,height=-1,copies=1;
					if(imageUrl!=null&&!"".equals(imageUrl)){
						//将fs:// ,widget://路径转成文件对应的本地路径
						String realPath=makeRealPath(imageUrl);			 
						x=moduleContext.optInt("x",0);
						y=moduleContext.optInt("y",0);
						width=moduleContext.optInt("width",-1);
						height=moduleContext.optInt("height",-1);
						copies=moduleContext.optInt("copies",1);
						Bitmap myBitmap = null;
		                try {     	 
		                	    //realPath="file:///storage/emulated/0/Tencent/QQ_Images/null-1d4672c6f7d3efa6.jpg";
		                	    if(!realPath.contains("file://")){
		                	    	realPath="file://"+realPath;
		                	    }               	   
		                	    Uri uri=getMediaUriFromPath(moduleContext.getContext(),realPath);
		    					if(uri!=null){    						 
		                            myBitmap = Media.getBitmap(moduleContext.getContext().getContentResolver(), uri);
		                            for(int i=0;i<copies;i++){
		                            	printer.printImage(new ZebraImageAndroid(myBitmap), x,y,width,height, false);
		                            }	    						                                  
		                            ret.put("result", true);
		            				ret.put("msg","打印成功");
		    					}else{
		        					ret.put("result", false);
		            				ret.put("msg","打印出错,打印的图片路径未找到!");
		        				}  					  				 
		                }catch (ConnectionException e) {
		                	ret.put("result", false);
		    				ret.put("msg","打印出错,请查看是否已经和打印机连接!");
		                } catch (FileNotFoundException e) {
		                	ret.put("result", false);
		    				ret.put("msg","打印出错,没有找到要打印的图片!");
		                } catch (IOException e) {
		                	ret.put("result", false);
		    				ret.put("msg","打印出错,读取图片失败!");
		                }finally{
		                	if(myBitmap!=null)
		                	  myBitmap.recycle();
		                }
					}else{
						ret.put("result", false);
						ret.put("msg","打印出错,未找到图片路径!");
					}			 					
				}catch(Exception ex){
					try{
						ret.put("result", false);
						ret.put("msg",ex.getMessage());
					}catch(Exception exx){
						
					}				
				}finally{
					moduleContext.success(ret, true);
				}
			}
			
		}).start();
		
    }
	//获取根据文件路径获取MediaUri(参考:https://www.jianshu.com/p/33bc363290e9)
	public static Uri getMediaUriFromPath(Context context, String path) {
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(mediaUri,
                null,
                MediaStore.Images.Media.DISPLAY_NAME + "= ?",
                new String[] {path.substring(path.lastIndexOf("/") + 1)},
                null);
        Uri uri = null;
        if(cursor.moveToFirst()) {
            uri = ContentUris.withAppendedId(mediaUri,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
        }
        cursor.close();
        return uri;
    }
	//关闭连接打印机
	public void jsmethod_closePrinter(final UZModuleContext moduleContext){
        //TODO
		new Thread(new Runnable(){	
			@Override
			public void run() {
				// TODO Auto-generated method stub
				JSONObject ret = new JSONObject();
				try{
					if(printer!=null){
						printer=null;
					}
					if(connection!=null){
						connection.close();
						connection=null;
					}
					ret.put("result", true);
					ret.put("msg","关闭成功!");
					
				}catch(Exception ex){		 
					try{
						ret.put("result", false);
						ret.put("msg",ex.getMessage());
					}catch(Exception exx){
						
					}		
				}finally{
					moduleContext.success(ret, true);
				}
			}
		}).start();	
    }
	//查找蓝牙失败调用的方法
	@Override
	public void discoveryError(String arg0) {
		// TODO Auto-generated method stub		 
	}
	//查找蓝牙完成调用的方法
	@Override
	public void discoveryFinished() {
		// TODO Auto-generated method stub		
	}
	//每次查找到蓝牙调用的方法
	@Override
	public void foundPrinter(DiscoveredPrinter printer) { 
        try{
        	if(printer instanceof DiscoveredPrinterBluetooth){
        		DiscoveredPrinterBluetooth device=(DiscoveredPrinterBluetooth)printer;
        		if(!printerAddress.contains(device.address)){
        		  printerAddress.add(device.address);
  				  JSONObject obj=new JSONObject();
  				  obj.put("address", device.address);
  				  obj.put("name", device.friendlyName);	
  				  printers.put(obj);
  			  }
        	}
        }catch(Exception ex){       	
        }		 
	}
}

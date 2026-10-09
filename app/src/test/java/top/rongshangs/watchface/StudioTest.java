package top.rongshangs.watchface;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.widget.*;
import android.view.View;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StudioTest {
 WatchfaceEngine engine;
 File artifacts;
 @Before public void before()throws Exception{engine=new WatchfaceEngine(RuntimeEnvironment.getApplication());artifacts=new File(System.getProperty("verification.dir"));assertTrue(artifacts.isDirectory()||artifacts.mkdirs());RuntimeEnvironment.getApplication().getSharedPreferences("studio",0).edit().clear().commit();new File(RuntimeEnvironment.getApplication().getFilesDir(),"photo.png").delete();}
 void png(Bitmap image,String name)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(artifacts,name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}}
 void equal(Bitmap a,Bitmap b){assertEquals(a.getWidth(),b.getWidth());assertEquals(a.getHeight(),b.getHeight());int[] x=new int[a.getWidth()*a.getHeight()],y=x.clone();a.getPixels(x,0,a.getWidth(),0,0,a.getWidth(),a.getHeight());b.getPixels(y,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());assertArrayEquals(x,y);}
 byte[] block(byte[] data,int h,int section,int id){int n=NativeFormat.u32(data,h+section),t=NativeFormat.u32(data,h+section+4);for(int i=0;i<n;i++)if(NativeFormat.u16(data,t+i*16)==id){int p=NativeFormat.u32(data,t+i*16+8),length=NativeFormat.u32(data,t+i*16+12);return Arrays.copyOfRange(data,p,p+length);}throw new AssertionError("missing");}
 @Test public void rleRejectsTruncationAndRoundTripsBoundaries(){Random r=new Random(20261009);for(int n:new int[]{0,1,2,3,126,127,128,254,255,1024,65535}){byte[] p=new byte[n];r.nextBytes(p);byte[] c=NativeFormat.rle(p);assertArrayEquals(p,NativeFormat.unrle(c,0,c.length,p.length));Arrays.fill(p,(byte)27);c=NativeFormat.rle(p);assertArrayEquals(p,NativeFormat.unrle(c,0,c.length,p.length));}assertThrows(IllegalArgumentException.class,()->NativeFormat.unrle(new byte[]{3},0,1,3));assertThrows(IllegalArgumentException.class,()->NativeFormat.unrle(new byte[]{(byte)130,1},0,2,2));}
 @Test public void defaultExportPreservesArtworkLayoutFontsAndAod()throws Exception{
  byte[] result=engine.export(null,new Crop(),WatchfaceEngine.GREEN,"若叶睦");Files.write(new File(artifacts,"default.bin").toPath(),result);WatchfaceEngine output=new WatchfaceEngine(result);equal(engine.singles.get(1),output.singles.get(1));
  for(int section:new int[]{8,32,64,80}){int n=NativeFormat.u32(engine.template,0xa8+section),t=NativeFormat.u32(engine.template,0xac+section);for(int i=0;i<n;i++){int id=NativeFormat.u16(engine.template,t+i*16);assertArrayEquals(block(engine.template,0xa8,section,id),block(result,0xa8,section,id));}}
  for(int section:new int[]{8,24,32,64,80}){int n=NativeFormat.u32(engine.template,0x148+section),t=NativeFormat.u32(engine.template,0x14c+section);for(int i=0;i<n;i++){int id=NativeFormat.u16(engine.template,t+i*16);assertArrayEquals(block(engine.template,0x148,section,id),block(result,0x148,section,id));}}
  Bitmap render=output.render(output.singles.get(1),WatchfaceEngine.GREEN,WatchfaceEngine.sample("2560","80"));png(render,"watchface-default.png");equal(Palette.quantize(render,WatchfaceEngine.GREEN,WatchfaceEngine.WHITE,WatchfaceEngine.INK),NativeFormat.single(result,NativeFormat.u32(result,32)));
  assertEquals(84,163-79);int bp=engine.groups.get(9),count=engine.template[bp+40]&255;assertEquals(76,NativeFormat.u16(engine.template,bp+46+8*count));assertEquals(0x124,NativeFormat.u32(engine.template,bp+48+8*count));assertEquals(0,NativeFormat.u32(engine.template,engine.widgets.get(4)+16));assertTrue(result.length<300000);
 }
 @Test public void photoAndLightColorExportKeepResourcesAndProduceValidPalette()throws Exception{
  Bitmap photo=BitmapFactory.decodeStream(getClass().getClassLoader().getResourceAsStream("default_art.png"));Crop crop=new Crop();crop.zoom=1.35f;crop.x=.44f;crop.y=.39f;crop.clamp(photo);int color=0xffc5ddc7;
  byte[] result=engine.export(photo,crop,color,"我的表盘");Files.write(new File(artifacts,"custom-light.bin").toPath(),result);WatchfaceEngine output=new WatchfaceEngine(result);int center=output.singles.get(1).getPixel(120,380);assertEquals(color,center);assertFalse(Arrays.equals(block(engine.template,0xa8,24,1),block(result,0xa8,24,1)));assertArrayEquals(block(engine.template,0xa8,32,0),block(result,0xa8,32,0));assertArrayEquals(block(engine.template,0xa8,80,9),block(result,0xa8,80,9));
  equal(engine.darkDigits[8],output.lists.get(4)[8]);equal(engine.darkBolt,output.singles.get(9));png(output.render(output.singles.get(1),color,WatchfaceEngine.sample("99999","100")),"watchface-custom.png");
  Bitmap small=Bitmap.createBitmap(466,466,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(small);canvas.drawColor(Color.BLACK);canvas.drawBitmap(output.singles.get(1),0,0,null);engine.foreground(canvas,color,WatchfaceEngine.sample("2560","80"));equal(small,output.render(output.singles.get(1),WatchfaceEngine.GREEN,WatchfaceEngine.sample("2560","80")));
 }
 @Test public void dynamicGroupsStayCenteredForAllBatteryAndStepLengths(){for(int battery=0;battery<=100;battery++){Map<String,String> values=WatchfaceEngine.sample("99999",Integer.toString(battery));int p=engine.groups.get(9),n=engine.template[p+40]&255,content=0;for(int i=0;i<n;i++){int child=NativeFormat.u32(engine.template,p+48+i*8);content+=engine.size(child&0xffffff,child>>>24,values)[0];}int left=83+(76-content)/2;assertTrue(left>=79);assertTrue(left+content<=163);assertTrue(Math.abs(left+content/2.-121)<=.5);}
  for(String steps:new String[]{"0","9","99","999","9999","99999"}){Map<String,String> values=WatchfaceEngine.sample(steps,"100");int p=engine.groups.get(8),n=engine.template[p+40]&255,content=0;for(int i=0;i<n;i++){int child=NativeFormat.u32(engine.template,p+48+i*8);content+=engine.size(child&0xffffff,child>>>24,values)[0];}assertTrue(content<=252);int left=107+(252-content)/2;assertTrue(Math.abs(left+content/2.-233)<=.5);}
 }
 @Test public void cropNeverExposesOutsideImage(){for(int[] wh:new int[][]{{100,100},{50,2000},{3000,40},{1024,1024}}){Bitmap p=Bitmap.createBitmap(wh[0],wh[1],Bitmap.Config.ARGB_8888);Crop c=new Crop();for(float x:new float[]{-10,.5f,10}){c.x=x;c.y=-x;c.zoom=10;c.clamp(p);Matrix inverse=new Matrix();assertTrue(c.matrix(p).invert(inverse));float[] pts={0,0,465,310};inverse.mapPoints(pts);assertTrue(pts[0]>=-.01);assertTrue(pts[1]>=-.01);assertTrue(pts[2]<=wh[0]+.01);assertTrue(pts[3]<=wh[1]+.01);}p.recycle();}}
 @Test public void activityLoadsAndRestoresCropAndColor()throws Exception{
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();assertEquals("top.rongshangs.watchface",activity.getPackageName());assertEquals(WatchfaceEngine.GREEN,activity.preview.color);
  View decor=activity.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1179,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2556,View.MeasureSpec.EXACTLY));decor.layout(0,0,1179,2556);Bitmap shot=Bitmap.createBitmap(1179,2556,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(shot));png(shot,"app-screen.png");
  activity.chooseButton.performClick();Intent request=Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_OPEN_DOCUMENT,request.getAction());assertEquals("image/*",request.getType());
  Bitmap photo=Bitmap.createBitmap(1000,1000,Bitmap.Config.ARGB_8888);try(FileOutputStream out=new FileOutputStream(activity.photoFile())){photo.compress(Bitmap.CompressFormat.PNG,100,out);}activity.preview.setPhoto(photo);activity.preview.crop.zoom=2.2f;activity.preview.crop.x=.42f;activity.preview.crop.y=.37f;activity.setColor(0xffb98777);activity.save();controller.pause().stop().destroy();
  ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).setup();MainActivity restored=second.get();assertEquals(0xffb98777,restored.preview.color);assertNotNull(restored.preview.photo);assertEquals(2.2f,restored.preview.crop.zoom,.001f);assertEquals(.42f,restored.preview.crop.x,.001f);assertEquals(.37f,restored.preview.crop.y,.001f);assertTrue(restored.zoom.isEnabled());second.pause().stop().destroy();
 }
 void await(java.util.function.BooleanSupplier condition)throws Exception{long deadline=System.currentTimeMillis()+15000;while(!condition.getAsBoolean()&&System.currentTimeMillis()<deadline){org.robolectric.shadows.ShadowLooper.idleMainLooper();Thread.sleep(15);}org.robolectric.shadows.ShadowLooper.idleMainLooper();assertTrue("background operation completed",condition.getAsBoolean());}
 @Test public void photoPickerAndDocumentSaveSurviveActivityRecreation()throws Exception{
  ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();File input=new File(artifacts,"picked.png");Bitmap photo=Bitmap.createBitmap(2400,1200,Bitmap.Config.ARGB_8888);new Canvas(photo).drawColor(0xff6e7b80);try(FileOutputStream out=new FileOutputStream(input)){photo.compress(Bitmap.CompressFormat.PNG,100,out);}photo.recycle();
  activity.onActivityResult(MainActivity.PICK_PHOTO,Activity.RESULT_CANCELED,new Intent());assertNull(activity.preview.photo);
  activity.onActivityResult(MainActivity.PICK_PHOTO,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(input)));await(()->!activity.busy);android.app.AlertDialog failure=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(failure==null?"no error":String.valueOf(((TextView)failure.findViewById(android.R.id.message)).getText()),activity.preview.photo);assertEquals(2048,activity.preview.photo.getWidth());assertEquals(1024,activity.preview.photo.getHeight());assertTrue(activity.photoFile().isFile());
  activity.setColor(0xff748b75);activity.prepareExport();await(()->!activity.busy);assertTrue(activity.pendingFile().isFile());Intent request=Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_CREATE_DOCUMENT,request.getAction());assertEquals("application/octet-stream",request.getType());assertTrue(request.getStringExtra(Intent.EXTRA_TITLE).endsWith("_S4.bin"));byte[] expected=Files.readAllBytes(activity.pendingFile().toPath());NativeFormat.validate(expected);
  controller.pause().stop().destroy();ActivityController<MainActivity> second=Robolectric.buildActivity(MainActivity.class).setup();MainActivity restored=second.get();File saved=new File(artifacts,"document-saved.bin");restored.onActivityResult(MainActivity.SAVE_BIN,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(saved)));await(()->!restored.busy);assertArrayEquals(expected,Files.readAllBytes(saved.toPath()));second.pause().stop().destroy();
 }
}

package top.rongshangs.watchface;

import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.util.*;

/** Preview and export use the same native resources and layout records. */
final class WatchfaceEngine {
 static final int GREEN=0xff2e5b58, WHITE=0xfff5f5f5, INK=0xff141b17;
 final byte[] template;
 final Map<Integer,Bitmap> singles=new HashMap<>();
 final Map<Integer,Bitmap[]> lists=new HashMap<>();
 final Map<Integer,Integer> widgets=new HashMap<>(),groups=new HashMap<>();
 final Bitmap lower;
 final Path portrait=portraitPath();
 final Bitmap darkBolt;
 final Bitmap[] darkDigits;
 static byte[] read(InputStream in)throws IOException {try(in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);return out.toByteArray();}}
 WatchfaceEngine(Context context)throws IOException {this(read(context.getAssets().open("template.bin")));}
 WatchfaceEngine(byte[] bytes){
  template=bytes;NativeFormat.validate(bytes);
  for(int section:new int[]{24,32,64,80}){int n=NativeFormat.u32(bytes,0xa8+section),t=NativeFormat.u32(bytes,0xac+section);
   for(int i=0;i<n;i++){int id=NativeFormat.u16(bytes,t+i*16),p=NativeFormat.u32(bytes,t+i*16+8);
    if(section==24)singles.put(id,NativeFormat.single(bytes,p));
    else if(section==32)lists.put(id,NativeFormat.list(bytes,p));
    else (section==64?widgets:groups).put(id,p);
   }
  }
  lower=Bitmap.createBitmap(singles.get(1),0,310,466,156);
  darkBolt=tint(singles.get(9),INK);Bitmap[] original=lists.get(4);darkDigits=new Bitmap[original.length];for(int i=0;i<original.length;i++)darkDigits[i]=tint(original[i],INK);
 }
 static boolean light(int color){double[] v={Color.red(color)/255.,Color.green(color)/255.,Color.blue(color)/255.};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return .2126*v[0]+.7152*v[1]+.0722*v[2]>.19;}
 static Bitmap tint(Bitmap src,int color){int w=src.getWidth(),h=src.getHeight();int[] p=new int[w*h];src.getPixels(p,0,w,0,0,w,h);for(int i=0;i<p.length;i++)p[i]=(p[i]&0xff000000)|(color&0xffffff);return Bitmap.createBitmap(p,w,h,Bitmap.Config.ARGB_8888);}
 static Path portraitPath(){
  // Circle to horizontal edge: a quintic transition matches position, tangent
  // and curvature at both joins. Mirrored on the left, with an 8x export mask.
  double a=.22,r=229,c=232.5,s=42;
  double[] x=new double[6],y=new double[6];x[0]=c+r*Math.cos(a);y[0]=c+r*Math.sin(a);
  x[1]=x[0]-Math.sin(a)*s/5;y[1]=y[0]+Math.cos(a)*s/5;
  x[2]=2*x[1]-x[0]-Math.cos(a)*s*s/r/20;y[2]=2*y[1]-y[0]-Math.sin(a)*s*s/r/20;
  x[5]=430;y[5]=310;x[4]=430+s/5;y[4]=310;x[3]=430+2*s/5;y[3]=310;
  Path p=new Path();int n=2000;for(int i=0;i<=n;i++){double t=Math.PI-a+(Math.PI+2*a)*i/n;float px=(float)(c+r*Math.cos(t)),py=(float)(c+r*Math.sin(t));if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}
  for(int i=1;i<=256;i++){double t=i/256.;p.lineTo((float)bezier(x,t),(float)bezier(y,t));}
  p.lineTo(35,310);for(int i=255;i>=0;i--){double t=i/256.;p.lineTo((float)(465-bezier(x,t)),(float)bezier(y,t));}p.close();return p;
 }
 static double bezier(double[] p,double t){double[] q=p.clone();for(int k=5;k>0;k--)for(int i=0;i<k;i++)q[i]=q[i]*(1-t)+q[i+1]*t;return q[0];}
 Bitmap coloredLower(int color){Bitmap out=lower.copy(Bitmap.Config.ARGB_8888,true);if(color==GREEN)return out;
  int[] p=new int[84*32];out.getPixels(p,0,84,79,56,84,32);
  for(int i=0;i<p.length;i++){int old=p[i];float k=Math.min(1,Math.max(0,(Color.red(old)*46+Color.green(old)*91+Color.blue(old)*88)/18141f));p[i]=Color.rgb(Math.round(Color.red(color)*k),Math.round(Color.green(color)*k),Math.round(Color.blue(color)*k));}
  out.setPixels(p,0,84,79,56,84,32);return out;
 }
 void background(Canvas canvas,Bitmap photo,Crop crop,int color,Bitmap bottom){
  if(photo==null)canvas.drawBitmap(singles.get(1),0,0,null);
  else{canvas.drawColor(Color.BLACK);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);BitmapShader shader=new BitmapShader(photo,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);shader.setLocalMatrix(crop.matrix(photo));paint.setShader(shader);canvas.drawPath(portrait,paint);}
  if(photo!=null||color!=GREEN)canvas.drawBitmap(bottom,0,310,null);
 }
 Bitmap exportBackground(Bitmap photo,Crop crop,int color){
  Bitmap bottom=coloredLower(color);
  if(photo==null){Bitmap out=singles.get(1).copy(Bitmap.Config.ARGB_8888,true);if(color!=GREEN)new Canvas(out).drawBitmap(bottom,0,310,null);bottom.recycle();Bitmap indexed=Palette.quantize(out,color,WHITE);out.recycle();return indexed;}
  int factor=Runtime.getRuntime().maxMemory()>=192L*1024*1024?8:4,w=466*factor,h=310*factor;
  Bitmap big=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(big);c.scale(factor,factor);c.drawColor(Color.BLACK);
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);BitmapShader shader=new BitmapShader(photo,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);shader.setLocalMatrix(crop.matrix(photo));p.setShader(shader);c.drawPath(portrait,p);
  int[] scan=new int[w*factor],small=new int[466*466];Arrays.fill(small,Color.BLACK);
  for(int y=0;y<310;y++){big.getPixels(scan,0,w,0,y*factor,w,factor);for(int x=0;x<466;x++){int rr=0,gg=0,bb=0;for(int yy=0;yy<factor;yy++)for(int xx=0;xx<factor;xx++){int rgb=scan[yy*w+x*factor+xx];rr+=Color.red(rgb);gg+=Color.green(rgb);bb+=Color.blue(rgb);}int n=factor*factor;small[y*466+x]=Color.rgb((rr+n/2)/n,(gg+n/2)/n,(bb+n/2)/n);}}
  big.recycle();bottom.getPixels(small,310*466,466,0,0,466,156);bottom.recycle();Bitmap out=Bitmap.createBitmap(small,466,466,Bitmap.Config.ARGB_8888);Bitmap indexed=Palette.quantize(out,color,WHITE);out.recycle();return indexed;
 }
 static Map<String,String> sample(String steps,String battery){String[] pairs={"0811","09","1011","28","0822","78","0821",steps,"0841",battery,"2012","5","1012","10","1A12","0","1912","9"};Map<String,String> values=new HashMap<>();for(int i=0;i<pairs.length;i+=2)values.put(pairs[i],pairs[i+1]);return values;}
 String value(int p,Map<String,String> values){String key=String.format(Locale.ROOT,"%02X%02X",template[p]&255,template[p+1]&255);String v=values.get(key);if(v==null)throw new IllegalArgumentException("未知数据源 "+key);if((template[p+3]&240)==16&&(template[p+3]&4)!=0){int count=template[p+2]&255;while(v.length()<count)v="0"+v;}return v;}
 int selected(int p,Map<String,String> values){int v=Integer.parseInt(value(p,values));if((NativeFormat.u32(template,p+12)&0x200)!=0){int n=lists.get(NativeFormat.u32(template,p+8)&0xffffff).length;for(int i=0;i<n;i++)if(NativeFormat.u32(template,p+16+i*4)==v)return i;throw new IllegalArgumentException("数据超出范围");}return v;}
 int[] size(int ref,int type,Map<String,String> values){
  if(type==2){Bitmap b=singles.get(ref);return new int[]{b.getWidth(),b.getHeight()};}
  if(type==9){int p=groups.get(ref),n=template[p+40]&255;return new int[]{NativeFormat.u16(template,p+46+n*8),NativeFormat.u16(template,p+44+n*8)};}
  if(type==7){int p=widgets.get(ref);Bitmap[] font=lists.get(NativeFormat.u32(template,p+8)&0xffffff);if((template[p+3]&240)==32){Bitmap b=font[selected(p,values)];return new int[]{b.getWidth(),b.getHeight()};}int w=font[0].getWidth()*value(p,values).length(),h=font[0].getHeight(),extra=NativeFormat.u32(template,p+16);if((extra>>>24)==2){Bitmap b=singles.get(extra&0xffffff);w+=b.getWidth();h=Math.max(h,b.getHeight());}return new int[]{w,h};}
  throw new IllegalArgumentException("不支持的布局元素");
 }
 void draw(Canvas c,int ref,int type,int x,int y,Map<String,String> values,boolean dark){
  if(type==2){c.drawBitmap(ref==9&&dark?darkBolt:singles.get(ref),x,y,null);return;}
  if(type==7){int p=widgets.get(ref),list=NativeFormat.u32(template,p+8)&0xffffff;Bitmap[] font=list==4&&dark?darkDigits:lists.get(list);if((template[p+3]&240)==32){c.drawBitmap(font[selected(p,values)],x,y,null);return;}int alignment=template[p+3]&3,width=size(ref,type,values)[0];x-=alignment==2?width/2:alignment==0?width:0;for(char digit:value(p,values).toCharArray()){Bitmap b=font[digit-'0'];c.drawBitmap(b,x,y,null);x+=b.getWidth();}int extra=NativeFormat.u32(template,p+16);if((extra>>>24)==2)c.drawBitmap(singles.get(extra&0xffffff),x,y,null);return;}
  if(type==9){int p=groups.get(ref),n=template[p+40]&255,height=size(ref,type,values)[1];boolean horizontal=(template[p+43]&240)==80;int flow=0;
   if(horizontal){int align=NativeFormat.u32(template,p+48+n*8)>>>2&7,content=0;for(int i=0;i<n;i++){int child=NativeFormat.u32(template,p+48+i*8);content+=size(child&0xffffff,child>>>24,values)[0];}int spare=Math.max(0,size(ref,type,values)[0]-content);x+=align==1?spare/2:align==2?spare:0;}
   for(int i=0;i<n;i++){int q=p+44+i*8,child=NativeFormat.u32(template,q+4),r=child&0xffffff,t=child>>>24,cx=x+(short)NativeFormat.u16(template,q),cy=y+(short)NativeFormat.u16(template,q+2);if(horizontal){int[] s=size(r,t,values);cx=x+flow;cy=y+(height-s[1])/2;flow+=s[0];}draw(c,r,t,cx,cy,values,dark);}return;
  }
  throw new IllegalArgumentException("不支持的布局元素");
 }
 void foreground(Canvas canvas,int color,Map<String,String> values){int count=NativeFormat.u32(template,0xb0),table=NativeFormat.u32(template,0xb4),base=NativeFormat.u32(template,table+8);for(int i=0;i<count;i++){int p=base+i*16,child=NativeFormat.u32(template,p);if((child>>>24)==2&&(child&0xffffff)==1)continue;draw(canvas,child&0xffffff,child>>>24,(short)NativeFormat.u16(template,p+4),(short)NativeFormat.u16(template,p+6),values,light(color));}}
 Bitmap render(Bitmap bg,int color,Map<String,String> values){Bitmap out=bg.copy(Bitmap.Config.ARGB_8888,true);foreground(new Canvas(out),color,values);return out;}
 byte[] export(Bitmap photo,Crop crop,int color,String title){Bitmap bg=exportBackground(photo,crop,color);Map<Integer,byte[]> replacements=new HashMap<>();replacements.put(24<<16|1,NativeFormat.encodeSingle(bg));if(light(color)){replacements.put(24<<16|9,NativeFormat.encodeSingle(darkBolt));replacements.put(32<<16|4,NativeFormat.encodeList(darkDigits));}Bitmap render=render(bg,color,sample("2560","80")),preview=Palette.quantize(render,color,WHITE,INK);byte[] output=NativeFormat.rebuild(template,replacements,NativeFormat.encodeSingle(preview),title);bg.recycle();render.recycle();preview.recycle();return output;}
}

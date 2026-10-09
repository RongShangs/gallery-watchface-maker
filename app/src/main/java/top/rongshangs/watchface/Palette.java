package top.rongshangs.watchface;

import android.graphics.Bitmap;
import java.util.*;

/** Indexed8 quantization with exact RGB nearest-color lookup (no coarse cache). */
final class Palette {
 static final class Cell {int count;long r,g,b;int color(){return (int)(r/count)<<16|(int)(g/count)<<8|(int)(b/count);}}
 static final class Box {
  ArrayList<Cell> cells;int axis,range;long count;
  Box(ArrayList<Cell> cells){this.cells=cells;int[] lo={255,255,255},hi={0,0,0};for(Cell c:cells){int rgb=c.color();for(int i=0;i<3;i++){int v=rgb>>(16-8*i)&255;lo[i]=Math.min(lo[i],v);hi[i]=Math.max(hi[i],v);}count+=c.count;}for(int i=0;i<3;i++)if(hi[i]-lo[i]>range){range=hi[i]-lo[i];axis=i;}}
  long priority(){return cells.size()<2?0:(long)(range*Math.sqrt(count));}
  int average(){long n=0,r=0,g=0,b=0;for(Cell c:cells){n+=c.count;r+=c.r;g+=c.g;b+=c.b;}return (int)(r/n)<<16|(int)(g/n)<<8|(int)(b/n);}
 }
 static int key(int c){return (c>>19&31)<<10|(c>>11&31)<<5|(c>>3&31);}
 static Bitmap quantize(Bitmap bitmap,int...reserved){
  int w=bitmap.getWidth(),h=bitmap.getHeight();int[] data=new int[w*h];bitmap.getPixels(data,0,w,0,0,w,h);
  LinkedHashSet<Integer> exact=new LinkedHashSet<>();exact.add(0);exact.add(0xffffff);for(int c:reserved)exact.add(c&0xffffff);
  HashSet<Integer> unique=new HashSet<>();for(int c:data){unique.add(c);if(unique.size()>256)break;}
  if(unique.size()<=256)return bitmap.copy(Bitmap.Config.ARGB_8888,false);
  HashMap<Integer,Cell> histogram=new HashMap<>();
  for(int pixel:data){int c=pixel&0xffffff;if(exact.contains(c))continue;Cell q=histogram.computeIfAbsent(key(c),k->new Cell());q.count++;q.r+=c>>16&255;q.g+=c>>8&255;q.b+=c&255;}
  PriorityQueue<Box> boxes=new PriorityQueue<>((a,b)->Long.compare(b.priority(),a.priority()));if(!histogram.isEmpty())boxes.add(new Box(new ArrayList<>(histogram.values())));
  while(boxes.size()<256-exact.size()&&!boxes.isEmpty()&&boxes.peek().priority()>0){
   Box box=boxes.remove();int axis=box.axis;box.cells.sort(Comparator.comparingInt(c->c.color()>>(16-axis*8)&255));
   long half=box.count/2,n=0;int mid=0;while(mid<box.cells.size()-1&&n<half)n+=box.cells.get(mid++).count;mid=Math.max(1,mid);
   boxes.add(new Box(new ArrayList<>(box.cells.subList(0,mid))));boxes.add(new Box(new ArrayList<>(box.cells.subList(mid,box.cells.size()))));
  }
  ArrayList<Integer> colors=new ArrayList<>(exact);while(!boxes.isEmpty())colors.add(boxes.remove().average());
  HashMap<Integer,Integer> cache=new HashMap<>();
  for(int i=0;i<data.length;i++){
   int c=data[i]&0xffffff,best=c;if(!exact.contains(c)){
    Integer hit=cache.get(c);if(hit!=null)best=hit;else{double distance=Double.MAX_VALUE;
     for(int p:colors){int r=(c>>16&255)-(p>>16&255),g=(c>>8&255)-(p>>8&255),b=(c&255)-(p&255);double d=2.0*r*r+3.0*g*g+b*b;if(d<distance){distance=d;best=p;}}cache.put(c,best);
    }
   }data[i]=0xff000000|best;
  }
  return Bitmap.createBitmap(data,w,h,Bitmap.Config.ARGB_8888);
 }
}

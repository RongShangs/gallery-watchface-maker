package top.rongshangs.watchface;

import android.graphics.Bitmap;
import android.graphics.Matrix;

final class Crop {
 float x=.5f,y=.5f,zoom=1;
 Crop(){}
 Crop(Crop c){x=c.x;y=c.y;zoom=c.zoom;}
 float base(Bitmap image){return Math.max(466f/image.getWidth(),310f/image.getHeight());}
 void clamp(Bitmap image){
  zoom=Math.max(1,Math.min(4,zoom));float scale=base(image)*zoom;
  float hx=233/(scale*image.getWidth()),hy=155/(scale*image.getHeight());
  x=Math.max(hx,Math.min(1-hx,x));y=Math.max(hy,Math.min(1-hy,y));
 }
 Matrix matrix(Bitmap image){float scale=base(image)*zoom;Matrix m=new Matrix();m.setScale(scale,scale);m.postTranslate(232.5f-x*image.getWidth()*scale,155-y*image.getHeight()*scale);return m;}
 void reset(){x=.5f;y=.5f;zoom=1;}
}

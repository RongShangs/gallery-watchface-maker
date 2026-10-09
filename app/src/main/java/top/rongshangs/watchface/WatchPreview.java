package top.rongshangs.watchface;

import android.content.Context;
import android.graphics.*;
import android.view.*;

final class WatchPreview extends View {
 final WatchfaceEngine engine;
 Bitmap photo,bottom;
 final Crop crop=new Crop();
 int color=WatchfaceEngine.GREEN;
 Runnable changed=()->{};
 final ScaleGestureDetector scale;
 float lastX,lastY;
 boolean adjusting;
 WatchPreview(Context context,WatchfaceEngine engine){super(context);this.engine=engine;bottom=engine.coloredLower(color);setContentDescription("表盘预览，可拖动图片和双指缩放");scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){if(photo!=null){crop.zoom*=d.getScaleFactor();crop.clamp(photo);invalidate();changed.run();}return true;}});}
 void setColor(int c){color=c;Bitmap previous=bottom;bottom=engine.coloredLower(c);if(previous!=null)previous.recycle();invalidate();}
 void setPhoto(Bitmap image){photo=image;crop.reset();if(image!=null)crop.clamp(image);invalidate();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth(),getHeight())/466f;c.save();c.translate((getWidth()-466*scale)/2,(getHeight()-466*scale)/2);c.scale(scale,scale);Path round=new Path();round.addCircle(233,233,233,Path.Direction.CW);c.clipPath(round);engine.background(c,photo,crop,color,bottom);engine.foreground(c,color,WatchfaceEngine.sample("2560","80"));c.restore();}
 @Override public boolean onTouchEvent(android.view.MotionEvent event){if(photo==null)return super.onTouchEvent(event);scale.onTouchEvent(event);float screenScale=Math.min(getWidth(),getHeight())/466f;
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:adjusting=true;lastX=event.getX();lastY=event.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;
   case MotionEvent.ACTION_MOVE:if(!scale.isInProgress()&&event.getPointerCount()==1){float s=crop.base(photo)*crop.zoom*screenScale;crop.x-=(event.getX()-lastX)/(photo.getWidth()*s);crop.y-=(event.getY()-lastY)/(photo.getHeight()*s);crop.clamp(photo);invalidate();changed.run();}lastX=event.getX();lastY=event.getY();return true;
   case MotionEvent.ACTION_POINTER_UP:int remaining=event.getActionIndex()==0?1:0;lastX=event.getX(remaining);lastY=event.getY(remaining);return true;
   case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:adjusting=false;getParent().requestDisallowInterceptTouchEvent(false);changed.run();performClick();return true;
  }return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
}

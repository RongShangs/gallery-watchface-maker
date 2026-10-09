package top.rongshangs.watchface;

import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

final class ColorPicker {
 interface Listener{void choose(int color);}
 static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static void show(Activity activity,int initial,Listener listener){
  float[] hsv=new float[3];Color.colorToHSV(initial,hsv);LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(activity,24),dp(activity,12),dp(activity,24),dp(activity,8));
  Plane plane=new Plane(activity,hsv);box.addView(plane,new LinearLayout.LayoutParams(-1,dp(activity,190)));
  SeekBar hue=new SeekBar(activity);hue.setMax(359);hue.setProgress((int)hsv[0]);box.addView(hue,new LinearLayout.LayoutParams(-1,dp(activity,52)));
  LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);View swatch=new View(activity);row.addView(swatch,new LinearLayout.LayoutParams(dp(activity,42),dp(activity,42)));
  EditText hex=new EditText(activity);hex.setSingleLine();hex.setTextSize(18);hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});hex.setHint("#2E5B58");LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(activity,16);row.addView(hex,hp);box.addView(row);
  boolean[] changing={false};Runnable update=()->{changing[0]=true;int color=Color.HSVToColor(hsv);hex.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff));GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(activity,12));swatch.setBackground(bg);changing[0]=false;};
  plane.changed=update;hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int progress,boolean user){if(user){hsv[0]=progress;plane.invalidate();update.run();}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
  hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){if(changing[0])return;String text=s.toString().trim();if(text.matches("#?[0-9a-fA-F]{6}")){int color=Color.parseColor(text.startsWith("#")?text:"#"+text);Color.colorToHSV(color,hsv);hue.setProgress((int)hsv[0]);plane.invalidate();GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(activity,12));swatch.setBackground(bg);}}public void afterTextChanged(Editable s){}});update.run();
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("电量区域颜色").setView(box).setNegativeButton("取消",null).setPositiveButton("使用这个颜色",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String text=hex.getText().toString().trim();if(!text.matches("#?[0-9a-fA-F]{6}")){hex.setError("请输入 6 位颜色值，例如 #2E5B58");return;}listener.choose(Color.parseColor(text.startsWith("#")?text:"#"+text));dialog.dismiss();}));dialog.show();
 }
 static final class Plane extends View {
  final float[] hsv;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Runnable changed=()->{};
  Plane(Context c,float[] values){super(c);hsv=values;setContentDescription("调整颜色饱和度与明度");}
  protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();paint.setShader(new LinearGradient(0,0,w,0,Color.WHITE,Color.HSVToColor(new float[]{hsv[0],1,1}),Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(new LinearGradient(0,0,0,h,Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(getContext(),2));paint.setColor(Color.BLACK);c.drawCircle(hsv[1]*w,(1-hsv[2])*h,dp(getContext(),8),paint);paint.setColor(Color.WHITE);c.drawCircle(hsv[1]*w,(1-hsv[2])*h,dp(getContext(),6),paint);paint.setStyle(Paint.Style.FILL);}
  public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_MOVE){hsv[1]=Math.max(0,Math.min(1,event.getX()/getWidth()));hsv[2]=1-Math.max(0,Math.min(1,event.getY()/getHeight()));invalidate();changed.run();getParent().requestDisallowInterceptTouchEvent(true);return true;}if(event.getActionMasked()==MotionEvent.ACTION_UP){performClick();return true;}return true;}
  public boolean performClick(){super.performClick();return true;}
 }
}

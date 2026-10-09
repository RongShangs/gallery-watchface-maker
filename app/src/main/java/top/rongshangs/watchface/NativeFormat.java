package top.rongshangs.watchface;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The template's indexed8 image format and relocation rules. No desktop APIs. */
final class NativeFormat {
    static int u16(byte[] b,int p){ range(b,p,2);return (b[p]&255)|(b[p+1]&255)<<8; }
    static int u32(byte[] b,int p){ range(b,p,4);return u16(b,p)|u16(b,p+2)<<16; }
    static void w16(byte[] b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);}
    static void w32(byte[] b,int p,int v){w16(b,p,v);w16(b,p+2,v>>>16);}
    static void range(byte[] b,int p,int n){if(p<0||n<0||p>b.length-n)throw new IllegalArgumentException("表盘资源不完整");}
    static int resource(byte[] b,int h,int section,int id){
        int n=u32(b,h+section),t=u32(b,h+section+4);
        for(int i=0;i<n;i++)if(u16(b,t+i*16)==id)return u32(b,t+i*16+8);
        throw new IllegalArgumentException("缺少表盘资源 "+id);
    }
    static boolean same(byte[] b,int p){return p+2<b.length&&b[p]==b[p+1]&&b[p]==b[p+2];}
    static byte[] rle(byte[] data){
        ByteArrayOutputStream out=new ByteArrayOutputStream();int i=0;
        while(i<data.length){
            if(same(data,i)){int n=1;while(n<127&&i+n<data.length&&data[i+n]==data[i])n++;out.write(n);out.write(data[i]&255);i+=n;}
            else{int n=1;while(n<127&&i+n<data.length&&!same(data,i+n))n++;out.write(n|128);out.write(data,i,n);i+=n;}
        }
        return out.toByteArray();
    }
    static byte[] unrle(byte[] b,int p,int length,int expected){
        range(b,p,length);if(expected<0||expected>16_000_000)throw new IllegalArgumentException("资源尺寸异常");
        byte[] out=new byte[expected];int end=p+length,q=0;
        while(p<end&&q<expected){int code=b[p++]&255,n=code&127;if(n==0||q+n>expected)throw new IllegalArgumentException("图片压缩数据异常");
            if((code&128)!=0){if(p+n>end)throw new IllegalArgumentException("图片数据截断");System.arraycopy(b,p,out,q,n);p+=n;}
            else{if(p>=end)throw new IllegalArgumentException("图片数据截断");Arrays.fill(out,q,q+n,b[p++]);}q+=n;
        }
        if(q!=expected||p!=end)throw new IllegalArgumentException("图片解压长度不符");return out;
    }
    static byte[] pixels(byte[] b,int p,int len){
        range(b,p,len);if(len>=8&&u32(b,p)==0x5aa521e0){int info=u32(b,p+4);if((info&15)!=1)throw new IllegalArgumentException("模板图片格式不支持");return unrle(b,p+8,len-8,info>>>4);}
        return Arrays.copyOfRange(b,p,p+len);
    }
    static Bitmap decode(byte[] b,int p,int len,int w,int h){
        if(w<=0||h<=0||w>4096||h>4096)throw new IllegalArgumentException("图片尺寸异常");
        byte[] raw=pixels(b,p,len);if(raw.length!=1024+w*h)throw new IllegalArgumentException("调色板数据长度不符");
        int[] colors=new int[256],argb=new int[w*h];for(int i=0;i<256;i++)colors[i]=u32(raw,i*4);
        for(int i=0;i<argb.length;i++)argb[i]=colors[raw[i+1024]&255];return Bitmap.createBitmap(argb,w,h,Bitmap.Config.ARGB_8888);
    }
    static Bitmap single(byte[] b,int p){if((b[p]&255)!=16)throw new IllegalArgumentException("模板图片格式不支持");return decode(b,p+12,u32(b,p+8),u16(b,p+4),u16(b,p+6));}
    static Bitmap[] list(byte[] b,int p){
        if((b[p]&255)!=16)throw new IllegalArgumentException("模板数字格式不支持");int n=b[p+1]&255,w=u16(b,p+4),h=u16(b,p+6);boolean compressed=(b[p+2]&255)==4;
        Bitmap[] out=new Bitmap[n];int q=p+12+(compressed?n*4:0),total=u32(b,p+8);
        for(int i=0;i<n;i++){int len=compressed?u32(b,p+12+i*4):total/n;out[i]=decode(b,q,len,w,h);q+=len;}return out;
    }
    static byte[] encodedPixels(Bitmap bitmap){
        int w=bitmap.getWidth(),h=bitmap.getHeight();int[] argb=new int[w*h];bitmap.getPixels(argb,0,w,0,0,w,h);
        LinkedHashMap<Integer,Integer> palette=new LinkedHashMap<>();byte[] raw=new byte[1024+argb.length];
        for(int i=0;i<argb.length;i++){int c=argb[i];Integer idx=palette.get(c);if(idx==null){idx=palette.size();if(idx>=256)throw new IllegalArgumentException("图片颜色未正确压缩");palette.put(c,idx);w32(raw,idx*4,c);}raw[1024+i]=(byte)(int)idx;}
        byte[] compressed=rle(raw),out=new byte[8+compressed.length];w32(out,0,0x5aa521e0);w32(out,4,(raw.length<<4)|1);System.arraycopy(compressed,0,out,8,compressed.length);return out;
    }
    static byte[] encodeSingle(Bitmap bitmap){
        byte[] pixels=encodedPixels(bitmap),out=new byte[12+pixels.length];out[0]=16;out[1]=4;w16(out,4,bitmap.getWidth());w16(out,6,bitmap.getHeight());w32(out,8,pixels.length);System.arraycopy(pixels,0,out,12,pixels.length);return out;
    }
    static byte[] encodeList(Bitmap[] bitmaps){
        byte[][] images=new byte[bitmaps.length][];int total=0;for(int i=0;i<images.length;i++){images[i]=encodedPixels(bitmaps[i]);total+=images[i].length;}
        byte[] out=new byte[12+images.length*4+total];out[0]=16;out[1]=(byte)images.length;out[2]=4;w16(out,4,bitmaps[0].getWidth());w16(out,6,bitmaps[0].getHeight());w32(out,8,total+images.length*4);
        int p=12+images.length*4;for(int i=0;i<images.length;i++){w32(out,12+i*4,images[i].length);System.arraycopy(images[i],0,out,p,images[i].length);p+=images[i].length;}return out;
    }
    static final class Buffer {
        byte[] b=new byte[1024*1024];int size;
        int append(byte[] block){int p=size;if(size+block.length>b.length)b=Arrays.copyOf(b,Math.max(b.length*2,size+block.length));System.arraycopy(block,0,b,size,block.length);size+=block.length;return p;}
        byte[] finish(){return Arrays.copyOf(b,size);}
    }
    static final class Table {int h,section,count,source,dest;Table(int h,int s,int n,int src,int dst){this.h=h;section=s;count=n;source=src;dest=dst;}}
    static byte[] rebuild(byte[] template,Map<Integer,byte[]> images,byte[] preview,String title){
        Buffer out=new Buffer();out.append(Arrays.copyOf(template,0x1e8));ArrayList<Table> tables=new ArrayList<>();
        int[] sections={8,16,24,32,40,48,56,64,72,80};
        for(int h:new int[]{0xa8,0x148})for(int s:sections){
            int n=u32(template,h+s),src=u32(template,h+s+4);range(template,src,n*16);
            int dst=out.append(Arrays.copyOfRange(template,src,src+n*16));w32(out.b,h+s+4,dst);tables.add(new Table(h,s,n,src,dst));
        }
        // The firmware walks the actual root records consecutively.
        for(Table t:tables)if(t.section==8)for(int i=0;i<t.count;i++){
            int p=u32(template,t.source+i*16+8),len=u32(template,t.source+i*16+12);if(len!=16)throw new IllegalArgumentException("模板根记录格式异常");
            int dst=out.append(Arrays.copyOfRange(template,p,p+len));w32(out.b,t.dest+i*16+8,dst);
        }
        Map<ByteBuffer,Integer> dedup=new HashMap<>();
        for(Table t:tables)if(t.section!=8)for(int i=0;i<t.count;i++){
            int def=t.source+i*16,p=u32(template,def+8),len=u32(template,def+12);range(template,p,len);
            int id=u16(template,def),key=t.section<<16|id;
            byte[] block=t.h==0xa8&&images.containsKey(key)?images.get(key):Arrays.copyOfRange(template,p,p+len);
            ByteBuffer k=ByteBuffer.wrap(block);Integer dst=dedup.get(k);if(dst==null){dst=out.append(block);dedup.put(k,dst);}
            w32(out.b,t.dest+i*16+8,dst);w32(out.b,t.dest+i*16+12,block.length);
        }
        int normal=out.append(preview);w32(out.b,32,normal);w32(out.b,0xa8+4,normal);
        int aod=u32(template,0x148+4);if(aod!=0){int len=12+u32(template,aod+8);range(template,aod,len);w32(out.b,0x148+4,out.append(Arrays.copyOfRange(template,aod,aod+len)));}
        byte[] name=title.getBytes(StandardCharsets.UTF_8);if(name.length>63||name.length==0)throw new IllegalArgumentException("表盘名称请使用 1～18 个字");
        Arrays.fill(out.b,0x68,0xa8,(byte)0);System.arraycopy(name,0,out.b,0x68,name.length);
        byte[] result=out.finish();validate(result);return result;
    }
    static void validate(byte[] b){
        for(int h:new int[]{0xa8,0x148}){
            int n=u32(b,h+8),t=u32(b,h+12),base=u32(b,t+8);
            for(int i=0;i<n;i++)if(u32(b,t+i*16+8)!=base+i*16)throw new IllegalArgumentException("表盘布局连续性校验失败");
            for(int s:new int[]{8,16,24,32,40,48,56,64,72,80}){
                int count=u32(b,h+s),table=u32(b,h+s+4);range(b,table,count*16);
                for(int i=0;i<count;i++){int p=u32(b,table+i*16+8),len=u32(b,table+i*16+12);range(b,p,len);}
            }
        }
    }
}

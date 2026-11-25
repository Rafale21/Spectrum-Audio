/*
This file is part of Spectrum Audio.
Copyright (C) 2026 No Ads Studio and contributors.
SPDX-License-Identifier: GPL-3.0-only

Spectrum Audio is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, version 3 only.
Spectrum Audio is distributed WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License in the repository root LICENSE file.
*/
package studio.noads.spectrumaudio;
import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
final class ImageExport {
    static void save(Context context,Bitmap image,String name)throws Exception {
        if(Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,name);values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/Spectrum Audio");values.put(MediaStore.Images.Media.IS_PENDING,1);Uri uri=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Impossible de créer l’image.");
            try{try(OutputStream out=context.getContentResolver().openOutputStream(uri,"w")){if(out==null||!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Écriture PNG impossible.");}values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);if(context.getContentResolver().update(uri,values,null,null)<1)throw new IOException("Publication PNG impossible.");}catch(Exception e){try{context.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}throw e;}
        }else{File directory=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Spectrum Audio");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Dossier Images inaccessible.");File target=new File(directory,name),partial=new File(directory,name+".part");try{try(OutputStream out=new FileOutputStream(partial)){if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Écriture PNG impossible.");}if(!partial.renameTo(target))throw new IOException("Finalisation PNG impossible.");}finally{partial.delete();}MediaScannerConnection.scanFile(context,new String[]{target.getAbsolutePath()},new String[]{"image/png"},null);}
    }
}

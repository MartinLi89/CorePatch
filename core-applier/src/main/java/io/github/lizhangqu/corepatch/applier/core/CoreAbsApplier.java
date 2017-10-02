package io.github.lizhangqu.corepatch.applier.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import io.github.lizhangqu.corepatch.applier.Applier;
import io.github.lizhangqu.corepatch.applier.ApplierException;

/**
 * 抽象的应用器
 *
 * @author lizhangqu
 * @version V1.0
 * @since 2017-10-02 22:00
 */
public abstract class CoreAbsApplier implements Applier {
    protected static final int LEVEL = 9;
    protected static final boolean NO_WRAP = true;
    protected static final int BUFFER_SIZE = 32768;

    private String getFileMD5(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        FileInputStream in = null;
        byte buffer[] = new byte[2048];
        int len;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 2048)) != -1) {
                digest.update(buffer, 0, len);
            }
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void verify(File oldFile, InputStream patchInputStream, OutputStream newOutputStream) throws ApplierException {
        if (!isSupport()) {
            throw new ApplierException("not support");
        }
        if (oldFile == null) {
            throw new ApplierException("oldRandomAccessFile == null");
        }
        if (!oldFile.exists()) {
            throw new ApplierException("oldFile not exists");
        }
        if (patchInputStream == null) {
            throw new ApplierException("patchInputStream == null");
        }
        if (newOutputStream == null) {
            throw new ApplierException("newOutputStream == null");
        }
    }

    private void verify(File oldFile, File patchFile, File newFile) throws ApplierException {
        if (!isSupport()) {
            throw new ApplierException("not support");
        }
        if (oldFile == null) {
            throw new ApplierException("oldFile == null");
        }
        if (patchFile == null) {
            throw new ApplierException("oldFile == null");
        }
        if (!oldFile.exists()) {
            throw new ApplierException("oldFile not exists");
        }
        if (!patchFile.exists()) {
            throw new ApplierException("patchFile not exists");
        }
        if (newFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            newFile.delete();
        }
        if (!newFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            newFile.getParentFile().mkdirs();
        }
        if (!newFile.exists()) {
            try {
                boolean newFileResult = newFile.createNewFile();
                if (!newFileResult) {
                    throw new ApplierException("create newFile failure");
                }
            } catch (IOException e) {
                throw new ApplierException("create newFile failure");
            }
        }
    }


    @Override
    public void apply(File oldFile, InputStream patchInputStream, OutputStream newOutputStream) throws ApplierException {
        verify(oldFile, patchInputStream, newOutputStream);
        Inflater uncompressor = null;
        InflaterInputStream patchInflaterInputStream = null;
        if (patchInputStream instanceof InflaterInputStream) {
            patchInflaterInputStream = (InflaterInputStream) patchInputStream;
        } else {
            uncompressor = new Inflater(NO_WRAP); // to compress the patch
            patchInflaterInputStream =
                    new InflaterInputStream(patchInputStream, uncompressor, BUFFER_SIZE);
        }
        try {
            applyPatch(oldFile, newOutputStream, patchInflaterInputStream);
            newOutputStream.flush();
        } catch (Exception e) {
            throw new ApplierException("apply failure");
        } finally {
            if (uncompressor != null) {
                uncompressor.end();
            }
            try {
                patchInflaterInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                newOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    protected abstract void applyPatch(File oldFile, OutputStream newOutputStream, InflaterInputStream patchInflaterInputStream) throws Exception;


    @Override
    public void apply(File oldFile, File patchFile, File newFile) throws ApplierException {
        verify(oldFile, patchFile, newFile);
        Inflater uncompressor = new Inflater(NO_WRAP);
        InflaterInputStream patchInflaterInputStream = null;
        try {
            patchInflaterInputStream = new InflaterInputStream(new FileInputStream(patchFile), uncompressor, BUFFER_SIZE);
            apply(oldFile, patchInflaterInputStream, new FileOutputStream(newFile));
        } catch (FileNotFoundException e) {
            throw new ApplierException("file not exist");
        } finally {
            uncompressor.end();
        }
    }


    @Override
    public String calculateMD5(File newFile) throws ApplierException {
        String fileMD5 = getFileMD5(newFile);
        if (fileMD5 == null || fileMD5.length() == 0) {
            throw new ApplierException("calculate md5 error");
        }
        return fileMD5;
    }
}

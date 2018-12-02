package me.shouheng.notepal.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.facebook.stetho.common.LogUtil;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.shouheng.commons.image.GifSizeFilter;
import me.shouheng.commons.utils.IntentUtils;
import me.shouheng.commons.utils.LogUtils;
import me.shouheng.commons.utils.PalmUtils;
import me.shouheng.commons.utils.ToastUtils;
import me.shouheng.data.ModelFactory;
import me.shouheng.data.entity.Attachment;
import me.shouheng.data.entity.Model;
import me.shouheng.data.model.enums.ModelType;
import me.shouheng.data.store.AttachmentsStore;
import me.shouheng.notepal.Constants;
import me.shouheng.notepal.PalmApp;
import me.shouheng.notepal.R;
import me.shouheng.notepal.activity.GalleryActivity;
import me.shouheng.notepal.activity.SketchActivity;
import me.shouheng.notepal.async.CreateAttachmentTask;
import me.shouheng.notepal.config.RequestCodes;
import me.shouheng.notepal.manager.FileManager;
import me.shouheng.notepal.util.preferences.NotePreferences;
import me.shouheng.notepal.util.preferences.PersistPreferences;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

import static android.app.Activity.RESULT_OK;

/**
 * Created by WngShhng (shouheng2015@gmail.com) on 2017/12/30.
 * Refactored by WngShhng (shouheng2015@gmail.com) on 2018/12/2. */
public class AttachmentHelper {

    /**
     * The common request code to take a photo.
     */
    private final static int REQUEST_CODE_TAKE_A_PHOTO = 0x1001;

    /**
     * The common request code to select images.
     */
    private final static int REQUEST_CODE_SELECT_IMAGES = 0x1002;

    /**
     * The common request code to take a video.
     */
    private final static int REQUEST_CODE_TAKE_A_VIDEO = 0x1003;

    /**
     * The common request code to pick files.
     */
    private final static int REQUEST_CODE_PICK_FILES = 0x1004;

    /**
     * The common request code to create a sketch.
     */
    private final static int REQUEST_CODE_CREATE_SKETCH = 0x1005;

    /**
     * The common request code to select images from the custom album.
     */
    private final static int REQUEST_CODE_SELECT_IMAGES_CUSTOM = 0x1006;

    /**
     * Won't compress the image when hit the size (KB).
     */
    private final static int COMPRESS_IGNORE_SIZE_KB = 100; // KB

    /**
     * The photo file path
     */
    private static String photoFilePath;


    // region attachment request methods

    /**
     * Pick images form album. This method will use the image phone gallery.
     * The {@link #pickFromAlbum(android.app.Fragment)} is the same, except that it requires the
     * fragment to handle the result.
     *
     * @param activity the activity to handle the result.
     */
    public static void pickFromAlbum(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGES);
    }

    public static void pickFromAlbum(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        fragment.startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGES);
    }

    /**
     * Pick images from the custom album. This method will use the {@link Matisse} as the album.
     * The {@link #pickFromCustomAlbum(Fragment)} is the same, except it requires the fragment
     * to handle the result.
     *
     * @param activity the activity to handle the result.
     */
    public static void pickFromCustomAlbum(Activity activity) {
        Matisse.from(activity)
                .choose(MimeType.ofImage())
                .theme(R.style.Matisse_Dracula)
                .countable(false)
                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .maxSelectable(9)
                .originalEnable(true)
                .maxOriginalSize(5)
                .imageEngine(new GlideEngine())
                .forResult(REQUEST_CODE_SELECT_IMAGES_CUSTOM);
    }

    public static void pickFromCustomAlbum(Fragment fragment) {
        Matisse.from(fragment)
                .choose(MimeType.ofImage())
                .theme(R.style.Matisse_Dracula)
                .countable(false)
                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .maxSelectable(9)
                .originalEnable(true)
                .maxOriginalSize(5)
                .imageEngine(new GlideEngine())
                .forResult(REQUEST_CODE_SELECT_IMAGES_CUSTOM);
    }

    /**
     * Take a photo, using the phone default camera.
     *
     * @param fragment the fragment to start camera
     */
    public static void takeAPhoto(Fragment fragment) {
        File file = FileManager.createNewAttachmentFile(fragment.getContext(), Constants.MIME_TYPE_IMAGE_EXTENSION);
        if (file == null) {
            ToastUtils.makeToast(R.string.text_failed_to_save_file);
            return;
        }
        String path = file.getPath();
        photoFilePath = path;
        Uri uri = FileManager.getUriFromFile(fragment.getContext(), new File(path));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        fragment.startActivityForResult(intent, REQUEST_CODE_TAKE_A_PHOTO);
    }

    /**
     * Create a sketch.
     *
     * @param fragment the fragment to start this request.
     */
    public static void createSketch(Fragment fragment) {
        File file = FileManager.createNewAttachmentFile(fragment.getContext(), Constants.MIME_TYPE_SKETCH_EXTENSION);
        if (file == null) {
            ToastUtils.makeToast(R.string.text_failed_to_save_file);
            return;
        }
        String path = file.getPath();
        photoFilePath = path;
        Intent intent = new Intent(fragment.getContext(), SketchActivity.class);
        intent.putExtra(SketchActivity.EXTRA_KEY_OUTPUT_FILE_PATH, path);
        fragment.startActivityForResult(intent, REQUEST_CODE_CREATE_SKETCH);
    }

    // endregion


    // region attachment result handler methods

    /**
     * Handle the activity result, call this method in your fragment's
     * {@link Fragment#onActivityResult(int, int, Intent)} method if you called one of the attachment
     * request method and want to handle the result automatically. Note that the fragment must
     * implement the {@link OnAttachingFileListener} interface.
     *
     * @param fragment the fragment to handle the result
     * @param requestCode the request code
     * @param resultCode the result code
     * @param data the data extras
     * @param model the model
     * @param <CTX> the fragment requirement
     * @param <M> the model requirement
     * @return the disposable
     */
    public static <CTX extends Fragment & OnAttachingFileListener, M extends Model> Disposable onActivityResult(
            CTX fragment, int requestCode, int resultCode, Intent data, M model) {
        if (requestCode == REQUEST_CODE_SELECT_IMAGES_CUSTOM && resultCode == RESULT_OK) {
            return handleCustomImagePickerResult(fragment, data, model);
        } else if (requestCode == REQUEST_CODE_TAKE_A_PHOTO && resultCode == RESULT_OK) {
            return handleTakeAPhotoResult(fragment, data, model);
        } else if (requestCode == REQUEST_CODE_CREATE_SKETCH && resultCode == RESULT_OK) {
            return handleSketchResult(fragment, data, model);
        }
        return null;
    }

    /**
     * Method used to handle attachments from the third part app send event.
     *
     * @param fragment the fragment to handle the result
     * @param uris the attachment uris
     * @param model the model
     * @param <CTX> the fragment requirement
     * @param <M> the model requirement
     * @return the disposable
     */
    public static <CTX extends Fragment & OnAttachingFileListener, M extends Model> Disposable handleAttachments(
            CTX fragment, List<Uri> uris, M model) {
        return Observable.fromIterable(uris)
                .map(uri -> {
                    Attachment attachment = FileManager.createAttachmentFromUri(fragment.getContext(), uri);
                    attachment.setModelCode(model.getCode());
                    attachment.setModelType(ModelType.getTypeByName(model.getClass()));
                    return attachment;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attachment -> {
                    if (Constants.MIME_TYPE_IMAGE.equals(attachment.getMineType())) {
                        /* Compress if the attachment was image. */
                        handleImageCompress(fragment, attachment, new DefaultCompressListener(fragment, attachment));
                    } else {
                        /* Save the attachment to database and notify the fragment. */
                        Observable.create((ObservableOnSubscribe<Attachment>) emitter -> {
                            AttachmentsStore.getInstance().saveModel(attachment);
                            emitter.onNext(attachment);
                        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(attachment1 -> {
                            if (PalmUtils.isAlive(fragment)) {
                                fragment.onAttachingFileFinished(attachment1);
                            }
                        });
                    }
                });
    }

    /**
     * Handle the images pick results from the custom image picker.
     *
     * @param fragment the fragment to resolve the result
     * @param data the data extras
     * @param model the model
     * @param <CTX> the fragment requirement
     * @param <M> the model type
     * @return the disposable
     */
    private static <CTX extends Fragment & OnAttachingFileListener, M extends Model> Disposable handleCustomImagePickerResult(
            CTX fragment, Intent data, M model) {
        return Observable.fromIterable(Matisse.obtainResult(data))
                .map(uri -> {
                    Attachment attachment = FileManager.createAttachmentFromUri(fragment.getContext(), uri);
                    attachment.setModelCode(model.getCode());
                    attachment.setModelType(ModelType.getTypeByName(model.getClass()));
                    return attachment;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .forEach(attachment -> handleImageCompress(fragment, attachment, new DefaultCompressListener(fragment, attachment)));
    }

    /**
     * Handle the photo capture result. This method will compress the image and save the attachment.
     *
     * @param fragment the fragment to resolve the result
     * @param data the data extras
     * @param model the model
     * @param <CTX> the fragment requirement
     * @param <M> the model type
     * @return the disposable
     */
    private static <CTX extends Fragment & OnAttachingFileListener, M extends Model> Disposable handleTakeAPhotoResult(
            CTX fragment, Intent data, M model) {
        return Observable
                .create((ObservableOnSubscribe<Attachment>) emitter -> {
                    Attachment attachment = ModelFactory.getAttachment();
                    attachment.setMineType(Constants.MIME_TYPE_IMAGE);
                    attachment.setPath(photoFilePath);
                    attachment.setModelCode(model.getCode());
                    attachment.setModelType(ModelType.getTypeByName(model.getClass()));
                    emitter.onNext(attachment);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attachment -> handleImageCompress(fragment, attachment, new DefaultCompressListener(fragment, attachment)));
    }

    /**
     * Handle the sketch request result. This method won't compress the sketch, and it will
     * directly call the callback fragment to notify the result.
     *
     * @param fragment the fragment to resolve the result
     * @param data the data extras
     * @param model the model
     * @param <CTX> the fragment requirement
     * @param <M> the model type
     * @return the disposable
     */
    private static <CTX extends Fragment & OnAttachingFileListener, M extends Model> Disposable handleSketchResult(
            CTX fragment, Intent data, M model) {
        return Observable
                .create((ObservableOnSubscribe<Attachment>) emitter -> {
                    Attachment attachment = ModelFactory.getAttachment();
                    attachment.setMineType(Constants.MIME_TYPE_IMAGE);
                    attachment.setPath(photoFilePath);
                    attachment.setModelCode(model.getCode());
                    attachment.setModelType(ModelType.getTypeByName(model.getClass()));
                    AttachmentsStore.getInstance().saveModel(attachment);
                    emitter.onNext(attachment);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attachment -> {
                    if (PalmUtils.isAlive(fragment)) {
                        fragment.onAttachingFileFinished(attachment);
                    }
                });
    }

    /**
     * Handle the image compress event for fragment.
     *
     * @param fragment the fragment type
     * @param attachment the attachment, will get the file path from it.
     * @param onCompressListener the compress listener
     * @param <CTX> the fragment requirement
     */
    private static <CTX extends Fragment & OnAttachingFileListener> void handleImageCompress(
            CTX fragment, Attachment attachment, OnCompressListener onCompressListener) {
        File file = new File(attachment.getPath());
        Luban.with(fragment.getContext())
                .load(file)
                .ignoreBy(COMPRESS_IGNORE_SIZE_KB)
                .setTargetDir(file.getParent())
                .setCompressListener(onCompressListener)
                .launch();
    }

    // endregion


    // region Resolve attachment clicking event.
    public static void resolveClickEvent(
            Context context,
            Attachment attachment,
            List<Attachment> attachments,
            String galleryTitle) {
        if (attachment == null) {
            ToastUtils.makeToast(R.string.text_file_not_exist);
            return;
        }
        switch (attachment.getMineType()) {
            case Constants.MIME_TYPE_FILES: {
                resolveFiles(context, attachment);
                break;
            }
            case Constants.MIME_TYPE_IMAGE:
            case Constants.MIME_TYPE_SKETCH:
            case Constants.MIME_TYPE_VIDEO: {
                resolveImages(context, attachment, attachments, galleryTitle);
                break;
            }
        }
    }

    private static void resolveFiles(Context context, Attachment attachment) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(attachment.getUri(), FileManager.getMimeType(context, attachment.getUri()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (IntentUtils.isAvailable(context.getApplicationContext(), intent, null)) {
            context.startActivity(intent);
        } else {
            ToastUtils.makeToast(R.string.text_failed_to_resolve_intent);
        }
    }

    private static void resolveImages(Context context,
                                      Attachment attachment,
                                      List<Attachment> attachments,
                                      String galleryTitle) {
        int clickedImage = 0;
        ArrayList<Attachment> images = new ArrayList<>();
        for (Attachment mAttachment : attachments) {
            if (Constants.MIME_TYPE_IMAGE.equals(mAttachment.getMineType())
                    || Constants.MIME_TYPE_SKETCH.equals(mAttachment.getMineType())
                    || Constants.MIME_TYPE_VIDEO.equals(mAttachment.getMineType())) {
                images.add(mAttachment);
                if (mAttachment.equals(attachment)) {
                    clickedImage = images.size() - 1;
                }
            }
        }
        Intent intent = new Intent(context, GalleryActivity.class);
        intent.putExtra(GalleryActivity.EXTRA_GALLERY_TITLE, galleryTitle);
        intent.putParcelableArrayListExtra(GalleryActivity.EXTRA_GALLERY_IMAGES, images);
        intent.putExtra(GalleryActivity.EXTRA_GALLERY_CLICKED_IMAGE, clickedImage);
        context.startActivity(intent);
    }

    // endregion

    // region Resolve attachment picking result.
    public static<T extends Fragment & OnAttachingFileListener> void resolveResult(T fragment, int requestCode, Intent data) {
        switch (requestCode){
            case RequestCodes.REQUEST_TAKE_PHOTO:
                getPhoto(fragment.getContext(), Constants.MIME_TYPE_IMAGE, new OnAttachingFileListener() {
                    @Override
                    public void onAttachingFileErrorOccurred(Attachment attachment) {
                        fragment.onAttachingFileErrorOccurred(attachment);
                    }

                    @Override
                    public void onAttachingFileFinished(Attachment attachment) {
                        if (PalmUtils.isAlive(fragment)) {
                            fragment.onAttachingFileFinished(attachment);
                        }
                    }
                });
                break;
            case RequestCodes.REQUEST_SELECT_IMAGE:
                startTask(fragment, data);
                break;
            case RequestCodes.REQUEST_TAKE_VIDEO:
                if (PalmUtils.isAlive(fragment)) {
                    fragment.onAttachingFileFinished(getVideo(data));
                }
                break;
            case RequestCodes.REQUEST_FILES:
                startTask(fragment, data);
                break;
            case RequestCodes.REQUEST_SKETCH:
                if (PalmUtils.isAlive(fragment)) {
                    fragment.onAttachingFileFinished(getSketch(Constants.MIME_TYPE_SKETCH));
                }
                break;
        }
    }

    public static<T extends Activity & OnAttachingFileListener> void resolveResult(T activity, int requestCode, Intent data) {
        switch (requestCode){
            case RequestCodes.REQUEST_TAKE_PHOTO:
                getPhoto(activity, Constants.MIME_TYPE_IMAGE, new OnAttachingFileListener() {
                    @Override
                    public void onAttachingFileErrorOccurred(Attachment attachment) {
                        activity.onAttachingFileErrorOccurred(attachment);
                    }

                    @Override
                    public void onAttachingFileFinished(Attachment attachment) {
                        if (PalmUtils.isAlive(activity)) {
                            activity.onAttachingFileFinished(attachment);
                        }
                    }
                });
                break;
            case RequestCodes.REQUEST_SELECT_IMAGE:
                startTask(activity, data);
                break;
            case RequestCodes.REQUEST_TAKE_VIDEO:
                if (PalmUtils.isAlive(activity)) {
                    activity.onAttachingFileFinished(getVideo(data));
                }
                break;
            case RequestCodes.REQUEST_FILES:
                startTask(activity, data);
                break;
            case RequestCodes.REQUEST_SKETCH:
                if (PalmUtils.isAlive(activity)) {
                    activity.onAttachingFileFinished(getSketch(Constants.MIME_TYPE_SKETCH));
                }
                break;
        }
    }

    private static void getPhoto(Context context, String mimeType, OnAttachingFileListener onAttachingFileListener) {
        Attachment photo = ModelFactory.getAttachment();
        photo.setMineType(mimeType);
        if (shouldCompressImage()) {
            compressImage(context, photo, new File(getPhotoFilePath()), onAttachingFileListener);
        } else {
            photo.setPath(getPhotoFilePath());
            photo.setUri(FileManager.getUriFromFile(context, new File(getPhotoFilePath())));
            if (onAttachingFileListener != null) {
                onAttachingFileListener.onAttachingFileFinished(photo);
            }
        }
    }

    private static boolean shouldCompressImage() {
        return NotePreferences.getInstance().isImageAutoCompress();
    }

    private static void compressImage(Context context,
                                      Attachment attachment,
                                      File var,
                                      OnAttachingFileListener onAttachingFileListener) {
//        Luban.with(context)
//                .load(var)
//                .ignoreBy(100)
//                .setTargetDir(var.getParent())
//                .setCompressListener(new DefaultCompressListener() {
//                    @Override
//                    public void onSuccess(File file) {
//                        FileManager.delete(PalmApp.getContext(), var.getPath());
//                        attachment.setPath(file.getPath());
//                        attachment.setUri(FileManager.getUriFromFile(PalmApp.getContext(), file));
//                        if (onAttachingFileListener != null) {
//                            onAttachingFileListener.onAttachingFileFinished(attachment);
//                        }
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        if (onAttachingFileListener != null) {
//                            onAttachingFileListener.onAttachingFileErrorOccurred(null);
//                        }
//                    }
//                })
//                .launch();
    }

    private static Attachment getSketch(String mimeType) {
        Attachment photo = ModelFactory.getAttachment();
        photo.setUri(FileManager.getUriFromFile(PalmApp.getContext(), new File(getPhotoFilePath())));
        photo.setMineType(mimeType);
        photo.setPath(getPhotoFilePath());
        return photo;
    }

    private static Attachment getVideo(Intent data) {
        Attachment attachment = ModelFactory.getAttachment();
        attachment.setUri(data.getData());
        attachment.setMineType(Constants.MIME_TYPE_VIDEO);
        attachment.setPath(getPhotoFilePath());
        return attachment;
    }

    private static <T extends Activity & OnAttachingFileListener> void startTask(T activity, Intent data) {
        for (Uri uri : getUrisFromIntent(data)) {
            new CreateAttachmentTask(activity, uri, activity).execute();
        }
    }

    private static <T extends Fragment & OnAttachingFileListener> void startTask(T fragment, Intent data) {
        for (Uri uri : getUrisFromIntent(data)) {
            new CreateAttachmentTask(fragment, uri, fragment).execute();
        }
    }

    private static List<Uri> getUrisFromIntent(Intent data) {
        List<Uri> uris = new ArrayList<>();
        if (PalmUtils.isJellyBean() && data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else {
            uris.add(data.getData());
        }
        return uris;
    }
    // endregion

    // region Start picking action.


    public static void pickFromAlbum(android.app.Fragment fragment) {
        fragment.startActivityForResult(pickFromAlbum(), RequestCodes.REQUEST_SELECT_IMAGE);
    }

    private static Intent pickFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        return intent;
    }

    public static void pickFiles(Activity activity) {
        activity.startActivityForResult(pickFiles(), RequestCodes.REQUEST_FILES);
    }

    public static void pickFiles(Fragment fragment) {
        fragment.startActivityForResult(pickFiles(), RequestCodes.REQUEST_FILES);
    }

    private static Intent pickFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (PalmUtils.isJellyBeanMR2()) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.setType("*/*");
        return intent;
    }

    public static void capture(Activity activity) {
        Intent intent = captureIntent(activity);
        if (intent == null) return;
        activity.startActivityForResult(intent, RequestCodes.REQUEST_TAKE_PHOTO);
    }

    public static void capture(Fragment fragment) {
        Intent intent = captureIntent(fragment.getContext());
        if (intent == null) return;
        fragment.startActivityForResult(intent, RequestCodes.REQUEST_TAKE_PHOTO);
    }

    @Nullable
    private static Intent captureIntent(Context context) {
        File file = FileManager.createNewAttachmentFile(context, Constants.MIME_TYPE_IMAGE_EXTENSION);
        if (file == null){
            ToastUtils.makeToast(R.string.text_failed_to_save_file);
            return null;
        }
        String filePath = file.getPath();
        setPhotoFilePath(file.getPath());
        Uri attachmentUri = FileManager.getUriFromFile(PalmApp.getContext(), new File(filePath));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
        return intent;
    }

    /**
     * TODO add the background bitmap logic, and refactor the result handler logic.
     *
     * @param activity the activity caller
     */
    public static void sketch(Activity activity) {
        Intent intent = sketchIntent(activity);
        if (intent == null) return;
        activity.startActivityForResult(intent, RequestCodes.REQUEST_SKETCH);
    }

    public static void sketch(Fragment fragment) {
        Intent intent = sketchIntent(fragment.getContext());
        if (intent == null) return;
        fragment.startActivityForResult(intent, RequestCodes.REQUEST_SKETCH);
    }

    @Nullable
    private static Intent sketchIntent(Context context) {
        File file = FileManager.createNewAttachmentFile(context, Constants.MIME_TYPE_SKETCH_EXTENSION);
        if (file == null) {
            ToastUtils.makeToast(R.string.text_failed_to_save_file);
            return null;
        }
        String filePath = file.getPath();
        setPhotoFilePath(filePath);
        Intent intent = new Intent(context, SketchActivity.class);
        intent.putExtra(SketchActivity.EXTRA_KEY_OUTPUT_FILE_PATH, filePath);
        return intent;
    }
    // endregion

    public static boolean checkAttachment(Attachment attachment) {
        LogUtils.d(attachment);
        if (attachment == null
                || attachment.getUri() == null
                || TextUtils.isEmpty(attachment.getUri().toString())) {
            ToastUtils.makeToast(R.string.text_failed_to_save_file);
            return false;
        }
        return true;
    }

    private static String getPhotoFilePath() {
        if (TextUtils.isEmpty(photoFilePath)) {
            photoFilePath = PersistPreferences.getInstance().getAttachmentFilePath();
        }
        return photoFilePath;
    }

    private static void setPhotoFilePath(String photoFilePath) {
        AttachmentHelper.photoFilePath = photoFilePath;
        PersistPreferences.getInstance().setAttachmentFilePath(photoFilePath);
    }

    /**
     * Yhe default image compress listener.
     * Will save the attachment and then call the callback fragment or callback activity.
     */
    private static class DefaultCompressListener implements OnCompressListener {

        private final static int ATTACHING_TYPE_FRAGMENT = 0;
        private final static int ATTACHING_TYPE_ACTIVITY = 1;

        private Fragment fragment;
        private Attachment attachment;
        private OnAttachingFileListener onAttachingFileListener;
        private int type;

        <CTX extends Fragment & OnAttachingFileListener> DefaultCompressListener(CTX fragment, Attachment attachment) {
            this.fragment = fragment;
            this.attachment = attachment;
            this.onAttachingFileListener = fragment;
            type = ATTACHING_TYPE_FRAGMENT;
        }

        @Override
        public void onStart() {
            LogUtil.d("start compress");
        }

        @Override
        public void onSuccess(File result) {
            /* If the original is compressed, delete it, otherwise keep it. */
            File file = new File(attachment.getPath());
            if (file.length() > (COMPRESS_IGNORE_SIZE_KB << 10)) {
                FileManager.delete(PalmApp.getContext(), file.getPath());
            }
            /* Save the attachment and call the callback. */
            switch (type) {
                case ATTACHING_TYPE_FRAGMENT:
                    Observable.create((ObservableOnSubscribe<Attachment>) emitter -> {
                        attachment.setPath(result.getPath());
                        attachment.setUri(FileManager.getUriFromFile(PalmApp.getContext(), result));
                        AttachmentsStore.getInstance().saveModel(attachment);
                        emitter.onNext(attachment);
                    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(attachment -> {
                        if (PalmUtils.isAlive(fragment)) {
                            onAttachingFileListener.onAttachingFileFinished(attachment);
                        }
                    });
                    break;
                case ATTACHING_TYPE_ACTIVITY:
                    break;
            }
        }

        @Override
        public void onError(Throwable e) {
            if (PalmUtils.isAlive(fragment)) {
                onAttachingFileListener.onAttachingFileErrorOccurred(attachment);
            }
        }
    }

    /**
     * Yhe attachment file handling callback
     */
    public interface OnAttachingFileListener {

        /**
         * Callback when error occurred when handling the attachment
         *
         * @param attachment the attachment handling
         */
        void onAttachingFileErrorOccurred(Attachment attachment);

        /**
         * Callback when handling finished
         *
         * @param attachment the attachment handling
         */
        void onAttachingFileFinished(Attachment attachment);
    }
}

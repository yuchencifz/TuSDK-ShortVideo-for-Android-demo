package org.lasque.tusdkvideodemo.editor;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.lasque.tusdk.api.video.retriever.TuSDKVideoImageExtractor;
import org.lasque.tusdk.core.TuSdk;
import org.lasque.tusdk.core.TuSdkContext;
import org.lasque.tusdk.core.common.TuSDKMediaDataSource;
import org.lasque.tusdk.core.decoder.TuSDKAudioDecoderTaskManager;
import org.lasque.tusdk.core.decoder.TuSDKVideoInfo;
import org.lasque.tusdk.core.seles.sources.TuSdkEditorPlayer;
import org.lasque.tusdk.core.seles.sources.TuSdkEditorSaver;
import org.lasque.tusdk.core.seles.sources.TuSdkEditorTranscoder;
import org.lasque.tusdk.core.seles.sources.TuSdkMovieEditor;
import org.lasque.tusdk.core.seles.sources.TuSdkMovieEditorImpl;
import org.lasque.tusdk.core.struct.TuSdkMediaDataSource;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.ThreadHelper;
import org.lasque.tusdk.video.editor.TuSdkMediaAudioEffectData;
import org.lasque.tusdk.video.editor.TuSdkMediaEffectData;
import org.lasque.tusdk.video.editor.TuSdkMediaStickerAudioEffectData;
import org.lasque.tusdkvideodemo.R;
import org.lasque.tusdkvideodemo.editor.component.EditorComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorEffectComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorFilterComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorHomeComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorMVComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorMusicComponent;
import org.lasque.tusdkvideodemo.editor.component.EditorTextComponent;
import org.lasque.tusdkvideodemo.utils.AppManager;
import org.lasque.tusdkvideodemo.views.MovieEditorTabBar;
import org.lasque.tusdkvideodemo.views.VideoContent;
import org.lasque.tusdkvideodemo.views.editor.EditorAnimator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import at.grabner.circleprogress.CircleProgressView;

/**
 * droid-sdk-video
 *
 * @author MirsFang
 * @Date 2018/9/25 11:37
 * @Copright (c) 2018 tusdk.com. All rights reserved.
 * <p>
 * 视频编辑控制类
 */
public class MovieEditorController {
    private static final String TAG = "MovieEditorController";
    //当前的视频编辑器
    private TuSdkMovieEditor mMovieEditor;
    //主音轨音量
    private float mMasterVolume = 0.5f;
    //音乐特效数据的备份
    private TuSdkMediaAudioEffectData mMusicEffectData;
    //MV特效数据
    private TuSdkMediaStickerAudioEffectData mMVEffectData;

    //当前Activity的引用
    private WeakReference<MovieEditorActivity> mWeakActivity;
    //视频View
    private VideoContent mHolderView;
    //播放按钮
    private ImageView mPlayBtn;
    //加载进度父视图
    private FrameLayout mProgressContent;
    //加载进度
    private CircleProgressView mProgress;
    //动画控制器
    private EditorAnimator mEditorAnimator;

    /*---------- 组件实例 ---------*/
    //当前正在使用的组件
    private EditorComponent mCurrentComponent;
    //主页面组件
    private EditorHomeComponent mHomeComponent;
    //滤镜组件
    private EditorFilterComponent mFilterComponent;
    //MV组件
    private EditorMVComponent mMVComponent;
    //音效组件
    private EditorMusicComponent mMusicComponent;
    //文字组件
    private EditorTextComponent mTextComponent;
    //特效组件
    private EditorEffectComponent mEffectComponent;
    //缩略图集合
    private List<Bitmap> mThumbBitmapList;
    //是否正在保存
    private boolean isSaving = false;

    /** 转码回调 **/
    private TuSdkEditorTranscoder.TuSdkTranscoderProgressListener mOnTranscoderProgressListener = new TuSdkEditorTranscoder.TuSdkTranscoderProgressListener() {
        @Override
        public void onProgressChanged(float percentage) {
            mProgress.setValue(percentage * 100);
        }

        @Override
        public void onLoadComplete(TuSDKVideoInfo outputVideoInfo, TuSdkMediaDataSource outputVideoSource) {
            mProgressContent.setVisibility(View.GONE);
            mHolderView.setClickable(true);
            mProgress.setValue(0);
            mPlayBtn.setVisibility(View.GONE);
            getHomeComponent().setEnable(true);
        }

        @Override
        public void onError(Exception e) {
            if (e != null) TLog.e(e);
            mProgressContent.setVisibility(View.GONE);
            mProgress.setValue(0);
            TuSdk.messageHub().showError(getActivity(), R.string.lsq_editor_load_error);
            getHomeComponent().setEnable(true);
        }
    };


    /** 播放回调 **/
    private TuSdkEditorPlayer.TuSdkProgressListener mPlayProgressListener = new TuSdkEditorPlayer.TuSdkProgressListener() {
        @Override
        public void onStateChanged(int state) {
            if (mCurrentComponent instanceof EditorHomeComponent) {
                mPlayBtn.setVisibility(state == 1 ? View.VISIBLE : View.GONE);
            }
        }

        @Override
        public void onProgress(long playbackTimeUs, long totalTimeUs, float percentage) {

        }
    };

    /** 保存回调 **/
    private TuSdkEditorSaver.TuSdkSaverProgressListener mSaveProgressListener = new TuSdkEditorSaver.TuSdkSaverProgressListener() {
        @Override
        public void onProgress(float progress) {
            mProgress.setValue(progress * 100);
        }

        @Override
        public void onCompleted(TuSdkMediaDataSource outputFile) {
            //文件保存路径为 outputFile.getPath()
            isSaving = false;
            TuSdk.messageHub().showSuccess(getActivity(), R.string.new_movie_saved);
            ThreadHelper.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            }, 1000);
            AppManager.getInstance().finishAllActivity();
        }

        @Override
        public void onError(Exception e) {
            isSaving = false;
            mProgressContent.setVisibility(View.GONE);
            mProgress.setValue(0);
            TuSdk.messageHub().showError(getActivity(), R.string.new_movie_error_saving);
            ThreadHelper.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            }, 1000);
        }
    };

    /**
     * 声音加载状态的回调
     */
    private TuSDKAudioDecoderTaskManager.TuSDKAudioDecoderTaskStateListener mAudioTaskStateListener = new TuSDKAudioDecoderTaskManager.TuSDKAudioDecoderTaskStateListener() {
        @Override
        public void onStateChanged(TuSDKAudioDecoderTaskManager.State state) {
            if (state == TuSDKAudioDecoderTaskManager.State.Complete) {
                TuSdk.messageHub().dismissRightNow();
                mMovieEditor.getEditorMixer().notifyLoadCompleted();
            }

        }
    };

    public MovieEditorController(MovieEditorActivity activity, String videoPath, VideoContent holderView, TuSdkMovieEditor.TuSdkMovieEditorOptions options) {
        mHolderView = holderView;
        mWeakActivity = new WeakReference<>(activity);
        mMovieEditor = new TuSdkMovieEditorImpl(activity, holderView, options);
        //设置数据源
        mMovieEditor.setDataSource(new TuSdkMediaDataSource(videoPath));
        //设置音效回调
        mMovieEditor.getEditorMixer().addTaskStateListener(mAudioTaskStateListener);
        //设置转码回调
        mMovieEditor.getEditorTransCoder().addTransCoderProgressListener(mOnTranscoderProgressListener);
        //设置播放回调
        mMovieEditor.getEditorPlayer().addProgressListener(mPlayProgressListener);
        //设置保存回调
        mMovieEditor.getEditorSaver().addSaverProgressListener(mSaveProgressListener);
        //初始化视图
        init();
        //之前在裁剪页面预加载过 则不用开启转码
        mMovieEditor.setEnableTranscode(false);
        mMovieEditor.loadVideo();
        //加载缩略图
        loadVideoThumbList(videoPath);
    }


    /**
     * 加载缩略图
     */
    private void loadVideoThumbList(String videoPath) {
        if (mThumbBitmapList == null) {
            mThumbBitmapList = new ArrayList<>();
            //设置输出的图片的大小
            TuSdkSize tuSdkSize = TuSdkSize.create(TuSdkContext.dip2px(56), TuSdkContext.dip2px(56));

            TuSDKVideoImageExtractor extractor = TuSDKVideoImageExtractor.createExtractor();
            extractor.setOutputImageSize(tuSdkSize);
            //设置视频数据源
            extractor.setVideoDataSource(TuSDKMediaDataSource.create(videoPath));
            //设置输出文件的数量(建议为20张  输出数量越多,则相应的生成图片的时间也会更长，内存占用更高)
            extractor.setExtractFrameCount(20);

            extractor.asyncExtractImageList(new TuSDKVideoImageExtractor.TuSDKVideoImageExtractorDelegate() {
                @Override
                public void onVideoImageListDidLoaded(List<Bitmap> images) {
                    /** 一次性拿到所有图片 只会回调一次 **/
                }

                @Override
                public void onVideoNewImageLoaded(Bitmap bitmap) {
                    /** 此方法是在每分离出一张图片就会回调一次  **/
                    mThumbBitmapList.add(bitmap);
                    getMVComponent().addCoverBitmap(bitmap);
                    getTextComponent().addCoverBitmap(bitmap);
                    getEffectComponent().addCoverBitmap(bitmap);
                }

            });
        }
    }

    /**
     * 获取缩略图列表
     *
     * @return
     */
    public List<Bitmap> getThumbBitmapList() {
        return mThumbBitmapList;
    }

    /**
     * 备忘音频特效数据
     *
     * @param mediaEffectData 音频特效数据
     */
    public void setMusicEffectData(TuSdkMediaAudioEffectData mediaEffectData) {
        this.mMusicEffectData = mediaEffectData;
    }

    /**
     * 获取音频备忘数据
     *
     * @return 获取备忘音频特效数据
     */
    public TuSdkMediaAudioEffectData getMusicEffectData() {
        return this.mMusicEffectData;
    }

    /**
     * 备忘MV特效数据
     *
     * @param mediaEffectData MV特效数据
     */
    public void setMVEffectData(TuSdkMediaStickerAudioEffectData mediaEffectData) {
        this.mMVEffectData = mediaEffectData;
    }

    /**
     * 获取MV备忘数据
     *
     * @return 获取备忘MV特效数据
     */
    public TuSdkMediaStickerAudioEffectData getMVEffectData() {
        return this.mMVEffectData;
    }

    /**
     * 获取最近一次备忘的音频/MV特效数据
     *
     * @return 获取备忘MV特效数据
     **/
    public TuSdkMediaEffectData getMediaEffectData() {
        return this.mMVEffectData == null ? mMusicEffectData : mMVEffectData;
    }

    /**
     * 获取主音量
     *
     * @return
     */
    public float getMasterVolume() {
        return mMasterVolume;
    }

    /**
     * 设置主音量
     *
     * @param volume
     */
    public void setMasterVolume(float volume) {
        this.mMasterVolume = volume;
    }

    /**
     * 初始化视图与动画控制器
     *
     * @since V3.0.0
     */
    private void init() {
        switchComponent(EditorComponent.EditorComponentType.Home);
        //初始化动画控制器(缩放预览图，改变底部视图宽高)
        mEditorAnimator = new EditorAnimator(this, mHolderView);

        mPlayBtn = getActivity().findViewById(R.id.lsq_play_btn);
        mProgressContent = getActivity().findViewById(R.id.lsq_editor_load);
        mProgress = getActivity().findViewById(R.id.lsq_editor_load_parogress);
        mHolderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayBtn.getVisibility() == View.GONE) {
                    mMovieEditor.getEditorPlayer().pausePreview();
                } else {
                    mMovieEditor.getEditorPlayer().startPreview();
                }
            }
        });
    }

    /**
     * 获取{@link MovieEditorActivity} 引用
     *
     * @return MovieEditorActivity Activity的引用
     * @since V3.0.0
     */
    public MovieEditorActivity getActivity() {
        if (mWeakActivity == null || mWeakActivity.get() == null) {
            TLog.e("%s MovieEditorActivity is null !!!", TAG);
            return null;
        }
        return mWeakActivity.get();
    }

    /**
     * 获取视频编辑器的实例 {@link TuSdkMovieEditor}
     *
     * @return TuSdkMovieEditor 视频编辑器的实例
     * @since V3.0.0
     */
    public TuSdkMovieEditor getMovieEditor() {
        if (mMovieEditor == null) {
            TLog.e("%s TuSdkMovieEditor is null !!!", TAG);
            return null;
        }
        return mMovieEditor;
    }

    /**
     * 获取顶部的头
     *
     * @return ViewGroup 头部的View
     */
    public ViewGroup getHeaderView() {
        return getActivity().getHeaderView();
    }

    /**
     * 获取底部的View
     *
     * @return ViewGroup底部的View
     */
    public ViewGroup getBottomView() {
        return getActivity().getBottomView();
    }

    /**
     * 获取视频的父View
     *
     * @since V3.0.0
     */
    public VideoContent getVideoContentView() {
        return mHolderView;
    }

    /**
     * 获取播放按钮
     *
     * @since V3.0.0
     */
    public ImageView getPlayBtn() {
        return mPlayBtn;
    }

    /**
     * 切换组件
     *
     * @since V3.0.0
     */
    public void switchComponent(EditorComponent.EditorComponentType componentEnum) {
        if (mCurrentComponent != null) mCurrentComponent.detach();
        switch (componentEnum) {
            case Home:
                //切换到主页面组件
                mCurrentComponent = getHomeComponent();
                break;
            case Filter:
                //切换到滤镜组件
                mCurrentComponent = getFilterComponent();
                break;
            case MV:
                //切换到MV组件
                mCurrentComponent = getMVComponent();
                break;
            case Music:
                //切换到配音组件
                mCurrentComponent = getMusicComponent();
                break;
            case Text:
                //切换到文字组件
                mCurrentComponent = getTextComponent();
                break;
            case Effect:
                //切换到特效组件(场景特效、时间特效、粒子特效)
                mCurrentComponent = getEffectComponent();
                break;
            default:
                break;
        }
        clearHeaderAndBottom();
        mCurrentComponent.attach();
    }

    /**
     * 清楚Header 和 Bottom 里的View
     *
     * @since V3.0.0
     **/
    private void clearHeaderAndBottom() {
        getHeaderView().removeAllViews();
        getBottomView().removeAllViews();
    }

    /**
     * 获取当前正在使用的组件
     * @return EditorComponent
     */
    public EditorComponent getCurrentComponent() {
        if (mCurrentComponent == null) {
            switchComponent(EditorComponent.EditorComponentType.Home);
        }
        return mCurrentComponent;
    }

    /** 保存视频 **/
    public void saveVideo() {
        setSaving(true);
        mProgressContent.setVisibility(View.VISIBLE);
        mPlayBtn.setVisibility(View.GONE);
        mHolderView.setClickable(false);
        mMovieEditor.saveVideo();
    }

    /**
     * 是否正在保存中
     *
     * @return true 正在保存 false 已经保存完毕或者出错
     * @since v 3.1.0
     */
    public boolean isSaving() {
        return isSaving;
    }

    /**
     * 设置正在保存的状态
     *
     * @return true 正在保存 false 已经保存完毕或者出错
     * @since v 3.1.0
     */
    private void setSaving(boolean isSaving) {
        this.isSaving = isSaving;
        if (isSaving) {
            getHomeComponent().setEnable(false);
            getPlayBtn().setClickable(false);
        }
    }

    /**
     * 获取Tab切换事件监听
     *
     * @return MovieEditorTabBarDelegate Tab切换事件监听委托
     * @since V3.0.0
     */
    public MovieEditorTabBar.MovieEditorTabBarDelegate getMovieEditorTabChangeListener() {
        return onMovieEditorTabChangeListener;
    }

    /**
     * Tab切换事件委托实例
     */
    private MovieEditorTabBar.MovieEditorTabBarDelegate onMovieEditorTabChangeListener = new MovieEditorTabBar.MovieEditorTabBarDelegate() {
        @Override
        public void onSelectedTabType(MovieEditorTabBar.TabType tabType) {
            TLog.d("%s select tab type is : %s", TAG, tabType);
            EditorComponent.EditorComponentType componentEnum = EditorComponent.EditorComponentType.Home;
            switch (tabType) {
                case FilterTab:
                    //当前已经切换为滤镜组件
                    componentEnum = EditorComponent.EditorComponentType.Filter;
                    break;
                case MVTab:
                    //当前已经切换为MV组件
                    componentEnum = EditorComponent.EditorComponentType.MV;
                    break;
                case MusicTab:
                    //当前已经切换为配音组件
                    componentEnum = EditorComponent.EditorComponentType.Music;
                    break;
                case TextTab:
                    //当前已经切换为文字组件
                    componentEnum = EditorComponent.EditorComponentType.Text;
                    break;
                case EffectTab:
                    //当前已经切换为特效组件 (场景特效、时间特效、粒子特效)
                    componentEnum = EditorComponent.EditorComponentType.Effect;
                    break;
                    default:
                        break;
            }
            if (mEditorAnimator != null) {
                mEditorAnimator.animatorSwitchComponent(componentEnum);
            } else {
                switchComponent(componentEnum);
            }

        }
    };

    /* ------------- 获取组件实例 --------------- */

    /**
     * 获取主页面组件
     *
     * @return EditorHomeComponent 主页组件实例
     * @since V3.0.0
     */
    public EditorHomeComponent getHomeComponent() {
        if (mHomeComponent == null) {
            mHomeComponent = new EditorHomeComponent(this);
            mHomeComponent.setEnable(false);
        }
        return mHomeComponent;
    }


    /**
     * 获取滤镜组件
     *
     * @return EditorFilterComponent
     * @since V3.0.0
     */
    public EditorFilterComponent getFilterComponent() {
        if (mFilterComponent == null) {
            mFilterComponent = new EditorFilterComponent(this);
        }
        return mFilterComponent;
    }

    /**
     * 获取MV组件
     *
     * @return EditorMVComponent
     * @since V3.0.0
     */
    public EditorMVComponent getMVComponent() {
        if (mMVComponent == null) {
            mMVComponent = new EditorMVComponent(this);
        }
        return mMVComponent;
    }

    /**
     * 获取音效组件
     *
     * @return EditorMusicComponent
     * @since V3.0.0
     */
    public EditorMusicComponent getMusicComponent() {
        if (mMusicComponent == null) {
            mMusicComponent = new EditorMusicComponent(this);
        }
        return mMusicComponent;
    }

    /**
     * 获取文字组件
     *
     * @return EditorTextComponent
     * @since V3.0.0
     */
    public EditorTextComponent getTextComponent() {
        if (mTextComponent == null) {
            mTextComponent = new EditorTextComponent(this);
            EditorTextComponent.EditorTextConfig textConfig = EditorTextComponent.EditorTextConfig.creat()
                    .setText("请输入文字")
                    .setTextColor("#ffffff")
                    .setTextSize(25)
                    .setTextPadding(20)
                    .setTextShadowColor("#fff222");
            mTextComponent.setTextConfig(textConfig);
        }
        return mTextComponent;
    }

    /**
     * 获取特效组件
     *
     * @return EditorEffectComponent
     * @since V3.0.0
     */
    public EditorEffectComponent getEffectComponent() {
        if (mEffectComponent == null) {
            mEffectComponent = new EditorEffectComponent(this);
        }
        return mEffectComponent;
    }



    /* ---------------------------- 同步Activity的生命周期 --------------------- */

    /**
     * 同步Activity的OnCreate
     *
     * @since V3.0.0
     */
    public void onCreate() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onCreate();
    }

    /**
     * 同步Activity的onStart
     *
     * @since V3.0.0
     */
    public void onStart() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onStart();
    }

    /**
     * 同步Activity的onResume
     *
     * @since V3.0.0
     */
    public void onResume() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onResume();
    }

    /**
     * 同步Activity的onPause
     *
     * @since V3.0.0
     */
    public void onPause() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onPause();
        if (!mMovieEditor.getEditorPlayer().isPause())
            mMovieEditor.getEditorPlayer().pausePreview();
    }

    /**
     * 同步Activity的onStop
     *
     * @since V3.0.0
     */
    public void onStop() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onStop();
    }

    /**
     * 同步Activity的onDestroy
     *
     * @since V3.0.0
     */
    public void onDestroy() {
        if (mCurrentComponent == null) return;
        mCurrentComponent.onDestroy();
        mMovieEditor.getEditorPlayer().destroy();
    }

    /**
     * 组件点击返回时间的回调
     */
    public void onBackEvent() {
        if (mCurrentComponent == null) {
            getActivity().finish();
            return;
        }
        switch (mCurrentComponent.getComponentEnum()) {
            case Home:
                getActivity().finish();
                break;
            case Filter:
            case MV:
            case Music:
            case Text:
            case Effect:
                if (mEditorAnimator != null) {
                    mEditorAnimator.animatorSwitchComponent(EditorComponent.EditorComponentType.Home);
                } else {
                    switchComponent(EditorComponent.EditorComponentType.Home);
                }

        }
    }

}
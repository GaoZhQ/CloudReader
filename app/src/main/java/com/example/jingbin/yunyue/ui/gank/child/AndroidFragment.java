package com.example.jingbin.yunyue.ui.gank.child;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;

import com.example.jingbin.yunyue.R;
import com.example.jingbin.yunyue.adapter.AndroidAdapter;
import com.example.jingbin.yunyue.app.Constants;
import com.example.jingbin.yunyue.base.BaseFragment;
import com.example.jingbin.yunyue.bean.GankIoDataBean;
import com.example.jingbin.yunyue.databinding.FragmentAndroidBinding;
import com.example.jingbin.yunyue.http.HttpUtils;
import com.example.jingbin.yunyue.http.cache.ACache;
import com.example.jingbin.yunyue.utils.DebugUtil;
import com.example.xrecyclerview.XRecyclerView;

import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 安卓社区和ios的fragment
 */
public class AndroidFragment extends BaseFragment<FragmentAndroidBinding> {

    private static final String TYPE = "mType";
    private String mType = "Android";
    private int mPage = 1;
    private boolean mIsPrepared;
    private boolean mIsFirst = true;
    private AndroidAdapter mAndroidAdapter;
    private ACache mACache;
    private GankIoDataBean mAndroidBean;

    public static AndroidFragment newInstance(String type) {
        AndroidFragment fragment = new AndroidFragment();
        Bundle args = new Bundle();
        args.putString(TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getString(TYPE);
        }
    }

    @Override
    public int setContent() {
        return R.layout.fragment_android;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mACache = ACache.get(getContext());
        mAndroidBean = (GankIoDataBean) mACache.getAsObject(Constants.GANK_ANDROID);
        DebugUtil.error("--AndroidFragment   ----onActivityCreated");
        bindingView.xrvAndroid.setPullRefreshEnabled(false);
        bindingView.xrvAndroid.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {

            }

            @Override
            public void onLoadMore() {
                mPage++;
                loadAndroidData();
            }
        });
        // 准备就绪
        mIsPrepared = true;
    }

    @Override
    protected void loadData() {
        if (!mIsPrepared || !mIsVisible || !mIsFirst) {
            return;
        }

        if (mAndroidBean != null
                && mAndroidBean.getResults() != null
                && mAndroidBean.getResults().size() > 0) {
            showContentView();
            setAdapter(mAndroidBean);
        } else {
            loadAndroidData();
        }
    }

    private void loadAndroidData() {
        Subscription subscribe = HttpUtils.getInstance().getGankIOServer().getGankIoData(mType, mPage, HttpUtils.per_page_more)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GankIoDataBean>() {
                    @Override
                    public void onCompleted() {
                        showContentView();
                    }

                    @Override
                    public void onError(Throwable e) {
                        bindingView.xrvAndroid.refreshComplete();
                        if (mPage == 1) {
                            showError();
                        }
                    }

                    @Override
                    public void onNext(GankIoDataBean gankIoDataBean) {
                        if (mPage == 1) {
                            if (gankIoDataBean != null && gankIoDataBean.getResults() != null && gankIoDataBean.getResults().size() > 0) {
                                setAdapter(gankIoDataBean);

                                mACache.remove(Constants.GANK_ANDROID);
                                // 缓存50分钟
                                mACache.put(Constants.GANK_ANDROID, gankIoDataBean, 3);
                            }
                        } else {
                            if (gankIoDataBean != null && gankIoDataBean.getResults() != null && gankIoDataBean.getResults().size() > 0) {
                                bindingView.xrvAndroid.refreshComplete();
                                mAndroidAdapter.addAll(gankIoDataBean.getResults());
                                mAndroidAdapter.notifyDataSetChanged();
                            } else {
                                bindingView.xrvAndroid.noMoreLoading();
                            }
                        }
                    }
                });
        addSubscription(subscribe);
    }

    /**
     * 设置adapter
     */
    private void setAdapter(GankIoDataBean mAndroidBean) {
        mAndroidAdapter = new AndroidAdapter();
        mAndroidAdapter.addAll(mAndroidBean.getResults());
        bindingView.xrvAndroid.setLayoutManager(new LinearLayoutManager(getActivity()));
        bindingView.xrvAndroid.setAdapter(mAndroidAdapter);
        mAndroidAdapter.notifyDataSetChanged();

        mIsFirst = false;
    }

    /**
     * 加载失败后点击后的操作
     */
    @Override
    protected void onRefresh() {
        loadAndroidData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DebugUtil.error("--AndroidFragment   ----onDestroy");
    }
}

package com.nicodelee.beautyarticle.ui.home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.nicodelee.beautyarticle.R;
import com.nicodelee.beautyarticle.adapter.MainRecyclerViewAdapter;
import com.nicodelee.beautyarticle.api.BeautyApi;
import com.nicodelee.beautyarticle.app.APP;
import com.nicodelee.beautyarticle.app.BaseFragment;
import com.nicodelee.beautyarticle.mode.ActicleMod;
import com.nicodelee.beautyarticle.mode.ActicleMod$Table;
import com.nicodelee.beautyarticle.utils.L;
import com.nicodelee.beautyarticle.viewhelper.EndlessRecyclerOnScrollListener;
import com.nicodelee.beautyarticle.viewhelper.MySwipeRefreshLayout;
import com.nicodelee.utils.ListUtils;
import com.nicodelee.utils.WeakHandler;
import com.raizlabs.android.dbflow.sql.language.Select;
import java.util.ArrayList;
import javax.inject.Inject;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ActicleListFragment extends BaseFragment
    implements SwipeRefreshLayout.OnRefreshListener {

  @Inject BeautyApi mbeautyApi;

  @Bind(R.id.recyclerview) RecyclerView rv;
  @Bind(R.id.swipe_container) MySwipeRefreshLayout mSwipeLayout;

  private ArrayList<ActicleMod> macticleMods;
  private MainRecyclerViewAdapter mActcleAdapter;
  private boolean isHasMore = true;
  private LinearLayoutManager linearLayoutManager;

  @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main_list, container, false);
    ButterKnife.bind(this, view);
    APP.from(getActivity()).getApplicationComponent().inject(this);
    return view;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    macticleMods = new ArrayList<ActicleMod>();
    mSwipeLayout.setOnRefreshListener(this);
    mSwipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorAccent,
        R.color.colorAccent, R.color.colorAccent);
    linearLayoutManager = new LinearLayoutManager(mActivity);
    rv.setLayoutManager(linearLayoutManager);
    setupRecyclerView();
    rv.addOnScrollListener(
        new EndlessRecyclerOnScrollListener(linearLayoutManager, APP.getInstance().imageLoader,
            false, true) {
          @Override public void onLoadMore() {
            int size = macticleMods.size();
            if (isHasMore && !mSwipeLayout.isRefreshing() && size > 0) {
              getActicle(1, (int) macticleMods.get(size - 1).id);
            }
          }
        });
  }

  private void setupRecyclerView() {

    if (isInDB()) {
      macticleMods = (ArrayList<ActicleMod>) new Select().from(ActicleMod.class)
          .orderBy(false, ActicleMod$Table.ID)
          .queryList();
      mActcleAdapter = new MainRecyclerViewAdapter(mActivity, macticleMods);
      rv.setAdapter(mActcleAdapter);
    } else {
      getActicle(0, 0);//首次获取数据
    }
  }

  private boolean isInDB() {
    return new Select().count().from(ActicleMod.class).count() > 0;
  }

  private void getActicle(final int page, int id) {

    mSwipeLayout.setRefreshing(true);

    //Subscription subscription = Observable.zip(mbeautyApi.getActicle(page,id),acticles -> );

    mbeautyApi.getActicle(page, id, new Callback<ArrayList<ActicleMod>>() {
      @Override public void success(final ArrayList<ActicleMod> acticleMods, Response response) {
        mSwipeLayout.setRefreshing(false);
        if (ListUtils.isEmpty(acticleMods)) {
          if (page > 0) {
            showToast("全部加载完毕");
            isHasMore = false;
          } else if (page < 0) {
            showToast("小编正为你编辑更多文章");
          }
          return;
        }

        //page = 0 首次 <0 刷新 >0 加载更多
        if (page == 0) {
          macticleMods = acticleMods;
          mActcleAdapter = new MainRecyclerViewAdapter(mActivity, macticleMods);
          rv.setAdapter(mActcleAdapter);
        } else if (page > 0) {
          macticleMods.addAll(acticleMods);
          mActcleAdapter.notifyItemInserted(macticleMods.size());
        } else if (page < 0) {
          for (ActicleMod mainMod : acticleMods) {
            macticleMods.add(0, mainMod);
          }
          //                            mActcleAdapter.notifyItemRangeChanged(0,acticleMods.size());
          mActcleAdapter.notifyDataSetChanged();
        }

        new WeakHandler().post(new Runnable() {
          @Override public void run() {
            ActicleMod acticleMod;
            for (ActicleMod mainMod : acticleMods) {
              acticleMod = mainMod;
              acticleMod.save();
            }
          }
        });
      }

      @Override public void failure(RetrofitError error) {
        mSwipeLayout.setRefreshing(false);
        L.e("error:" + error + ",url:" + error.getUrl());
      }
    });
  }

  @Override public void onRefresh() {
    new WeakHandler().postDelayed(new Runnable() {
      @Override public void run() {
        if (ListUtils.isEmpty(macticleMods)) {
          getActicle(0, 0);//first time
        } else {
          getActicle(-1, (int) macticleMods.get(0).id);//update
        }
      }
    }, 300);
  }

  public void onEvent(String msg) {
    if (msg.equals("Reselected")) {
      linearLayoutManager.scrollToPosition(0);
    } else if (msg.equals("clear")) {
      mActcleAdapter.clearData();
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    ButterKnife.unbind(this);
  }
}

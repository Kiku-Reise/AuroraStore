package com.dragons.aurora.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.dragons.aurora.AppListIterator;
import com.dragons.aurora.PlayStoreApiAuthenticator;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.EndlessAppsAdapter;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.CategoryAppsIterator;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.playstore.CategoryAppsTask;

import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TopFreeApps extends CategoryAppsTask {

    private boolean setLooper = true;
    private boolean loading = true;
    private int oldItems, visibleItems, totalItems;
    private View view;
    private AppListIterator iterator;
    private RecyclerView recyclerView;
    private EndlessAppsAdapter endlessAppsAdapter;
    private Disposable disposable;

    public AppListIterator getIterator() {
        return iterator;
    }

    public void setIterator(AppListIterator iterator) {
        this.iterator = iterator;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.app_endless_inc, container, false);
        setRecyclerView(view.findViewById(R.id.endless_apps_list));
        setIterator(setupIterator(CategoryAppsFragment.categoryId, GooglePlayAPI.SUBCATEGORY.TOP_FREE));
        fetchCategoryApps(false);
        return view;
    }

    protected AppListIterator setupIterator(String categoryId, GooglePlayAPI.SUBCATEGORY subcategory) {
        AppListIterator iterator;
        try {
            iterator = new AppListIterator(new CategoryAppsIterator(
                    new PlayStoreApiAuthenticator(getContext()).getApi(),
                    categoryId,
                    subcategory));
            iterator.setFilter(new FilterMenu(getContext()).getFilterPreferences());
            return iterator;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void setupListView(List<App> appsToAdd) {
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this.getActivity());
        endlessAppsAdapter = new EndlessAppsAdapter(getActivity(), appsToAdd);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this.getActivity(), R.anim.layout_anim));
        recyclerView.setAdapter(endlessAppsAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if ((visibleItems + oldItems) >= totalItems + 2)
                    setLooper = false;
                if (dy > 0) {
                    visibleItems = mLayoutManager.getChildCount();
                    totalItems = mLayoutManager.getItemCount();
                    oldItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (loading && !setLooper) {
                        if ((visibleItems + oldItems) >= totalItems - 2) {
                            loading = false;
                            fetchCategoryApps(true);
                        }
                    }
                }
            }
        });
        getLooper();
    }

    public void fetchCategoryApps(boolean shouldIterate) {
        disposable = Observable.fromCallable(() -> getResult(iterator))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appList -> {
                    if (shouldIterate) {
                        loading = true;
                        addApps(appList);
                    } else
                        setupListView(appList);
                }, this::processException);
    }

    public void addApps(List<App> appsToAdd) {
        if (!appsToAdd.isEmpty()) {
            for (App app : appsToAdd)
                endlessAppsAdapter.add(app);
            endlessAppsAdapter.notifyItemInserted(endlessAppsAdapter.getItemCount() - 1);
        }
        getLooper();
    }

    public void getLooper() {
        if (iterator.hasNext() && setLooper)
            fetchCategoryApps(true);
    }
}
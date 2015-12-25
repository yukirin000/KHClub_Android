package com.app.khclub.news.ui.activity;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.app.khclub.R;
import com.app.khclub.base.adapter.HelloHaAdapter;
import com.app.khclub.base.adapter.HelloHaBaseAdapterHelper;
import com.app.khclub.base.helper.JsonRequestCallBack;
import com.app.khclub.base.helper.LoadDataHandler;
import com.app.khclub.base.manager.HttpManager;
import com.app.khclub.base.manager.UserManager;
import com.app.khclub.base.ui.activity.BaseActivityWithTopBar;
import com.app.khclub.base.utils.KHConst;
import com.app.khclub.base.utils.KHUtils;
import com.app.khclub.base.utils.ToastUtil;
import com.app.khclub.news.ui.model.BetterMembersModel;
import com.app.khclub.news.ui.model.CirclePageModel;
import com.app.khclub.news.ui.model.NoticeModel;
import com.app.khclub.personal.ui.activity.OtherPersonalActivity;
import com.easemob.chat.InitSmackStaticCode;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class AnnouncementActivity extends BaseActivityWithTopBar {
	// 下拉模式
	public static final int PULL_DOWM_MODE = 0;
	// 上拉模式
	public static final int PULL_UP_MODE = 1;
	// 是否下拉刷新
	private boolean isPullDown = false;
	// 当前的数据页
	private int currentPage = 1;
	// 是否是最后一页数据
	private String lastPage = "0";
	// 是否正在请求数据
	private boolean isRequestingData = false;
	// 是否是最后一页
	private CirclePageModel circleData;
	@ViewInject(R.id.announcement_listView)
	//公告listview
	private PullToRefreshListView noticeListView;
	private ImageView rightBtn;
	//公告列表适配器
	private HelloHaAdapter noticeModelAdapter;
    private List<NoticeModel> datalist;
	@Override
	public int setLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.activity_announcement;
	}

	@Override
	protected void setUpView() {
		rightBtn = addRightImgBtn(R.layout.right_image_button, R.id.layout_top_btn_root_view, R.id.img_btn_right_top);
		rightBtn.setImageResource(R.drawable.revise);
		rightBtn.setVisibility(View.GONE);
		sendNotice();
		circleData = (CirclePageModel) getIntent().getSerializableExtra("data");

		//InitListView();
	}

	private void sendNotice() {
		// TODO Auto-generated method stub
		if ((UserManager.getInstance().getUser().getUid() + "").equals(circleData.getUserID())) {
			rightBtn.setVisibility(View.VISIBLE);

			rightBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					// 跳转发布页面
					Intent intent = new Intent(AnnouncementActivity.this, PublisAnnouncementActivity.class);
					intent.putExtra("data", circleData);
					startActivityWithRight(intent);
				}
			});
		}
	}

	private void InitListView() {
		// TODO Auto-generated method stub

		noticeModelAdapter = new HelloHaAdapter<NoticeModel>(AnnouncementActivity.this,
				R.layout.activity_announcement_item) {
			
			@Override
			protected void convert(HelloHaBaseAdapterHelper helper, final NoticeModel item) {
				// item 位置
				// position = helper.getPosition();
				// if (position == 0) {
				// helper.setVisible(R.id.all_club_members_layout, true);
				// } else {
				// helper.setVisible(R.id.all_club_members_layout, false);
				// }
				// helper.setText(R.id.member_user_name, item.getName());
				// if ("".equals(item.getJob())) {
				// helper.setText(R.id.member_job, "暂无信息");
				// } else {
				// helper.setText(R.id.member_job, item.getJob());
				// }
				// ImageView userImageView =
				// helper.getView(R.id.member_user_image);
				// ImageLoader.getInstance().displayImage(KHConst.ATTACHMENT_ADDR
				// + item.getHead_sub_image(),
				// userImageView, headImageOptions);
			}
		};

		// 适配器绑定
		noticeListView.setAdapter(noticeModelAdapter);
		noticeListView.setMode(Mode.BOTH);
		noticeListView.setPullToRefreshOverScrollEnabled(false);
		/**
		 * 刷新监听
		 */
		noticeListView.setOnRefreshListener(new OnRefreshListener2<ListView>() {

			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				if (!isRequestingData) {
					isRequestingData = true;
					currentPage = 1;
					isPullDown = true;
					getData(currentPage);
				}
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				if (!lastPage.equals("1") && !isRequestingData) {
					isRequestingData = true;
					isPullDown = false;
					getData(currentPage);
				}
			}
		});
		/**
		 * 设置底部自动刷新
		 */
		noticeListView.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {

			@Override
			public void onLastItemVisible() {
				if (!lastPage.equals("1")) {
					noticeListView.setMode(Mode.PULL_FROM_END);
					noticeListView.setRefreshing(true);
				}
			}
		});

		// 快宿滑动时不加载图片
//		noticeModelAdapter.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));

//		noticeModelAdapter.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				Intent intentToUsrMain = new Intent(AnnouncementActivity.this, OtherPersonalActivity.class);
//				BetterMembersModel membersModel = noticeModelAdapter.getItem(position - 1);
//				intentToUsrMain.putExtra(OtherPersonalActivity.INTENT_KEY,
//						KHUtils.stringToInt(membersModel.getUser_id()));
//				startActivityWithRight(intentToUsrMain);
			}
	//	});

	//}
	private void getData(int page) {
		
		//String circleid = getIntent().getStringExtra("circle_id");
		String path = KHConst.GET_NOTICE_LIST + "?circle_id=" + circleData.getCircleId() +"&user_id="+circleData.getUserID()+ "&page="
				+ page;
		HttpManager.get(path, new JsonRequestCallBack<String>(new LoadDataHandler<String>() {
			@Override
			public void onSuccess(JSONObject jsonResponse, String flag) {
				super.onSuccess(jsonResponse, flag);
				int status = jsonResponse.getInteger(KHConst.HTTP_STATUS);
				if (status == KHConst.STATUS_SUCCESS) {
					JSONObject jResult = jsonResponse.getJSONObject(KHConst.HTTP_RESULT);
					// 获取动态列表
					String followJsonArray = jResult.getString("list");
					datalist = JSON.parseArray(followJsonArray, NoticeModel.class);
//					// 如果是下拉刷新
//					if (isPullDown) {
//						noticeModelAdapter.replaceAll(dataList);
//					} else {
//						noticeModelAdapter.addAll(dataList);
//					}
					
					noticeListView.onRefreshComplete();
					// 是否是最后页
					lastPage = jResult.getString("is_last");
					if (lastPage.equals("0")) {
						currentPage++;
						noticeListView.setMode(Mode.BOTH);
					} else {
						noticeListView.setMode(Mode.PULL_FROM_START);
					}
					isRequestingData = false;					

				}
				
				if (status == KHConst.STATUS_FAIL) {
					noticeListView.onRefreshComplete();
					if (lastPage.equals("0")) {
						noticeListView.setMode(Mode.BOTH);
					}
					ToastUtil.show(AnnouncementActivity.this,jsonResponse.getString(KHConst.HTTP_MESSAGE));
				}
				isRequestingData = false;
			}

			@Override
			public void onFailure(HttpException arg0, String arg1, String flag) {
				super.onFailure(arg0, arg1, flag);
				noticeListView.onRefreshComplete();
				if (lastPage.equals("0")) {
					noticeListView.setMode(Mode.BOTH);
				}
				ToastUtil.show(AnnouncementActivity.this,getString(R.string.net_error));
				isRequestingData = false;
			}

		}, null));
	}

}

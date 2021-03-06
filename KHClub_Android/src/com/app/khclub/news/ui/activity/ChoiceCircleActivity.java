package com.app.khclub.news.ui.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.app.khclub.R;
import com.app.khclub.base.adapter.HelloHaAdapter;
import com.app.khclub.base.adapter.HelloHaBaseAdapterHelper;
import com.app.khclub.base.helper.JsonRequestCallBack;
import com.app.khclub.base.helper.LoadDataHandler;
import com.app.khclub.base.manager.HttpManager;
import com.app.khclub.base.manager.UserManager;
import com.app.khclub.base.model.UserModel;
import com.app.khclub.base.ui.activity.BaseActivityWithTopBar;
import com.app.khclub.base.ui.activity.MainTabActivity;
import com.app.khclub.base.utils.KHConst;
import com.app.khclub.base.utils.LogUtils;
import com.app.khclub.base.utils.ToastUtil;
import com.app.khclub.news.ui.model.CircleItemModel;
import com.app.khclub.news.ui.model.NewsConstants;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ChoiceCircleActivity extends BaseActivityWithTopBar {

	// 已选的圈子ID
	public static final String INTENT_CIRCLE_ID = "circleID";
	// 内容
	public static String INTENT_CONTENT_TEXT = "content_text";
	// 位置
	public static String INTENT_LOCATION = "location";
	// 图片
	public static String INTENT_IMAGES = "images";
	@ViewInject(R.id.follow_circle_list)
	private ListView listView;
	// 被选中的圈子位置数组
	private List<String> choiceList = new ArrayList<String>();
	// private int index;
	private String content_text;
	private String location;
	private ArrayList<String> images;
	// 没有数据时显示的部分
	@ViewInject(R.id.please_choice_circle)
	private LinearLayout linearLayout;
	// 跳转至选择圈子的按钮
	@ViewInject(R.id.skip_cirlce_fragemt)
	private TextView skipTextView;
	// 位置listview的适配器
	private HelloHaAdapter<CircleItemModel> circleAdapter;
	// 图片配置
	private DisplayImageOptions options;
	// 已选圈子
	private String choicedCirlce;
	// 正在提交中的标示
	private Boolean isUpload = false;
	// 是否关注有圈子
	private boolean isHaveFollowCircle = false;

	@OnClick(value = { R.id.base_ll_right_btns })
	private void clickEvent(View view) {
		switch (view.getId()) {
		case R.id.base_ll_right_btns:
			if (isHaveFollowCircle) {
				publishNews();
			} else {
				jumpCreateCircle();
			}
			break;
		default:
			break;
		}
	}

	private void jumpCreateCircle() {
		// TODO Auto-generated method stub
		startActivityWithRight(new Intent(ChoiceCircleActivity.this, CreateCircleActivity.class));
	}

	@Override
	public int setLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.activity_choice_circle;
	}

	@Override
	protected void setUpView() {
		setBarText(getString(R.string.choice_circle));
		// TODO Auto-generated method stub
		// 数据获取
		Intent intent = getIntent();
		content_text = intent.getStringExtra(INTENT_CONTENT_TEXT);
		location = intent.getStringExtra(INTENT_LOCATION);
		images = intent.getStringArrayListExtra(INTENT_IMAGES);
		// 有圈子
		if (intent.hasExtra(INTENT_CIRCLE_ID)) {
			choicedCirlce = intent.getStringExtra(INTENT_CIRCLE_ID);
		}

		// 显示图片的配置
		options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.loading_default)
				.showImageOnFail(R.drawable.loading_default).cacheInMemory(true).cacheOnDisk(true)
				.bitmapConfig(Bitmap.Config.RGB_565).build();

		initList();
		getData();
	}

	private void initList() {
		// 设置内容
		circleAdapter = new HelloHaAdapter<CircleItemModel>(this, R.layout.adapter_choice_circle) {
			@Override
			protected void convert(final HelloHaBaseAdapterHelper helper, final CircleItemModel item) {
				// 圈子标题
				helper.setText(R.id.circle_name, item.getCircle_name());
				helper.setText(R.id.category_name, item.getCategory_name());
				helper.setText(R.id.circle_people_count, item.getFollow_quantity());
				ImageView headImageView = helper.getView(R.id.circle_image);
				String image = item.getCircle_cover_sub_image();
				// //是否选中处理
				// for (CircleItemModel model : choice) {
				// if (item.getId().equals(model.getId())) {
				// helper.getView(R.id.change_circle).setVisibility(View.VISIBLE);
				// break;
				// }else {
				// helper.getView(R.id.change_circle).setVisibility(View.GONE);
				// }
				// }
				// 是否选中处理
				if (choiceList.contains(helper.getPosition() + "")) {
					helper.getView(R.id.change_circle).setVisibility(View.VISIBLE);
				} else {
					helper.getView(R.id.change_circle).setVisibility(View.GONE);
				}
				// 加入图片
				if (image.length() > 0) {
					ImageLoader.getInstance().displayImage(KHConst.ATTACHMENT_ADDR + image, headImageView, options);
				} else {
					headImageView.setImageResource(R.drawable.loading_default);
				}

				// if (choiceList.contains(helper.getPosition()+"")) {
				// helper.getView(R.id.change_circle).setVisibility(View.GONE);
				// }else {
				// helper.getView(R.id.change_circle).setVisibility(View.VISIBLE);
				// }

				helper.getView().setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (null != choicedCirlce && choicedCirlce.equals(item.getId())) {
							// 默认选中的不可以取消
							return;
						}
						if (choiceList.contains(helper.getPosition() + "")) {
							choiceList.remove(helper.getPosition() + "");
							helper.getView(R.id.change_circle).setVisibility(View.GONE);
							// index--;
						} else {
							choiceList.add(helper.getPosition() + "");
							// if
							// (choiceList.get(index).equals(helper.getPosition()+""))
							// {
							// index++;
							helper.getView(R.id.change_circle).setVisibility(View.VISIBLE);
							// }else {
							// helper.getView(R.id.change_circle).setVisibility(View.GONE);
							// }

							// helper.getView().setBackgroundResource(R.color.main_light_gary);
						}
					}
				});
			}
		};

		// 适配器绑定
		listView.setAdapter(circleAdapter);

	}

	/**
	 * 获所有收藏的名片信息
	 */
	private void getData() {
		String path = KHConst.GET_MY_FOLLOW_CIRCLE_LIST + "?" + "&user_id="
				+ UserManager.getInstance().getUser().getUid();
		// Log.i("wwww", path);
		LogUtils.i("path=" + path);
		HttpManager.get(path, new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

			@Override
			public void onSuccess(JSONObject jsonResponse, String flag) {
				super.onSuccess(jsonResponse, flag);
				int status = jsonResponse.getInteger(KHConst.HTTP_STATUS);
				if (status == KHConst.STATUS_SUCCESS) {
					JSONObject jResult = jsonResponse.getJSONObject(KHConst.HTTP_RESULT);
					// 获取数据列表
					 List<CircleItemModel> list =
					 JSON.parseArray(jResult.getString("list"),
					 CircleItemModel.class);
					// 模拟没有数据时候
					//List<CircleItemModel> list = new ArrayList<CircleItemModel>();
					// 有选中的
					if (null != choicedCirlce) {
						CircleItemModel tmpModel = null;
						for (CircleItemModel circleItemModel : list) {
							if (choicedCirlce.equals(circleItemModel.getId())) {
								tmpModel = circleItemModel;
								break;
							}
						}
						// 顺序修改
						if (null != tmpModel) {
							list.remove(tmpModel);
							list.add(0, tmpModel);
							choiceList.add("0");
						}
					}

					circleAdapter.replaceAll(list);

					if (list.size() == 0) {
						isHaveFollowCircle=false;
						linearLayout.setVisibility(View.VISIBLE);
						skip();
					} else {
						isHaveFollowCircle=true;
						linearLayout.setVisibility(View.GONE);
						// 添加完成按钮
						TextView sendBtn = addRightBtn(getResources().getString(R.string.publish_news));
						sendBtn.setTextColor(getResources().getColor(R.color.main_white));
					}
				}

				if (status == KHConst.STATUS_FAIL) {

				}
			}

			@Override
			public void onFailure(HttpException arg0, String arg1, String flag) {
				super.onFailure(arg0, arg1, flag);
			}
		}, null));
	}

	private void skip() {
		// TODO Auto-generated method stub
		ImageView rightBtn = addRightImgBtn(R.layout.right_image_button, R.id.layout_top_btn_root_view,
				R.id.img_btn_right_top);
		rightBtn.setImageResource(R.drawable.create_cirlce_bnt);
		skipTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivityWithRight(new Intent(ChoiceCircleActivity.this, MainTabActivity.class));
			}
		});

	}

	private void publishNews() {

		if (isUpload) {
			return;
		}

		if (choiceList.isEmpty()) {
			ToastUtil.show(this, R.string.choice_circle_none);
			return;
		}

		final UserModel userModel = UserManager.getInstance().getUser();
		// showLoading(getResources().getString(R.string.uploading), false);
		RequestParams params = new RequestParams();
		// 用户id
		params.addBodyParameter("uid", userModel.getUid() + "");
		// 内容
		params.addBodyParameter("content_text", content_text);
		// location
		params.addBodyParameter("location", location);

		for (int i = 0; i < images.size(); i++) {
			String path = images.get(i);
			// 图片
			File file = new File(path);
			if (file.exists()) {
				params.addBodyParameter("image" + i, file);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < choiceList.size(); i++) {
			String position = choiceList.get(i);
			CircleItemModel circleItemModel = circleAdapter.getItem(Integer.parseInt(position));
			sb.append(circleItemModel.getId() + ",");
		}
		if (sb.length() > 0) {
			sb.delete(sb.length() - 1, sb.length());
		}
		params.addBodyParameter("circles", sb.toString());

		isUpload = true;
		// 发布
		HttpManager.post(KHConst.PUBLISH_NEWS, params, new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

			@Override
			public void onSuccess(JSONObject jsonResponse, String flag) {
				super.onSuccess(jsonResponse, flag);
				isUpload = false;
				hideLoading();
				int status = jsonResponse.getIntValue("status");
				switch (status) {
				case KHConst.STATUS_SUCCESS:
					publishFinishBroadcast();
					// toast
					ToastUtil.show(ChoiceCircleActivity.this, R.string.news_publish_success);
					hideLoading();
					finishWithRight();
					break;
				case KHConst.STATUS_FAIL:
					hideLoading();
					Toast.makeText(ChoiceCircleActivity.this, R.string.news_publish_fail, Toast.LENGTH_SHORT).show();
					break;
				}
			}

			@Override
			public void onFailure(HttpException arg0, String arg1, String flag) {
				super.onFailure(arg0, arg1, flag);
				isUpload = false;
				hideLoading();
				Toast.makeText(ChoiceCircleActivity.this, R.string.net_error, Toast.LENGTH_SHORT).show();
			}
		}, null));

	}

	private void publishFinishBroadcast() {
		Intent mIntent = new Intent(KHConst.BROADCAST_NEWS_LIST_REFRESH);
		mIntent.putExtra(NewsConstants.PUBLISH_FINISH, "");
		// 发送广播
		LocalBroadcastManager.getInstance(ChoiceCircleActivity.this).sendBroadcast(mIntent);
	}
}

package com.app.khclub.message.ui.activity;

/**
 * 添加联系人好友
 * */
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.app.khclub.R;
import com.app.khclub.base.adapter.HelloHaAdapter;
import com.app.khclub.base.adapter.HelloHaBaseAdapterHelper;
import com.app.khclub.base.easeim.KHHXSDKHelper;
import com.app.khclub.base.easeim.activity.AlertDialog;
import com.app.khclub.base.easeim.applib.controller.HXSDKHelper;
import com.app.khclub.base.easeim.domain.User;
import com.app.khclub.base.helper.JsonRequestCallBack;
import com.app.khclub.base.helper.LoadDataHandler;
import com.app.khclub.base.manager.HttpManager;
import com.app.khclub.base.manager.UserManager;
import com.app.khclub.base.ui.activity.BaseActivityWithTopBar;
import com.app.khclub.base.utils.KHConst;
import com.app.khclub.base.utils.KHUtils;
import com.app.khclub.base.utils.ToastUtil;
import com.app.khclub.message.ui.model.PersonModel;
import com.app.khclub.personal.ui.activity.OtherPersonalActivity;
import com.easemob.chat.EMContactManager;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.lidroid.xutils.bitmap.BitmapDisplayConfig;
import com.lidroid.xutils.bitmap.callback.BitmapLoadFrom;
import com.lidroid.xutils.bitmap.callback.DefaultBitmapLoadCallBack;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ContactsUserActivity extends BaseActivityWithTopBar {

	// 获取库Phon表字段
	private static final String[] PHONES_PROJECTION = new String[] {
			Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID };
	// 联系人显示名称
	private static final int PHONES_DISPLAY_NAME_INDEX = 0;
	// 电话号码
	private static final int PHONES_NUMBER_INDEX = 1;
	// 联系人名称
	private ArrayList<String> mContactsName = new ArrayList<String>();
	// 联系人号码
	private ArrayList<String> mContactsNumber = new ArrayList<String>();
	// 联系人listview
	@ViewInject(R.id.listview_contacts_user)
	private PullToRefreshListView contactsListView;
	// 提示信息
	@ViewInject(R.id.tv_contacts_prompt)
	private TextView promptTextView;
	// 数据源
	private List<PersonModel> dataList = new ArrayList<PersonModel>();
	// 适配器
	private HelloHaAdapter<PersonModel> contactsAdapter;
	// 加载图片
	private ImageLoader imgLoader;
	// 图片配置
	private DisplayImageOptions options;

	@Override
	public int setLayoutId() {
		return R.layout.activity_add_contacts_user;
	}

	@Override
	protected void setUpView() {
		init();
		getPhoneContacts();
		if (isCanUseSim()) {
			// 判断SIM卡是否可用
			getSIMContacts();
		}
		listviewSet();

		if (mContactsNumber.size() > 0) {
			// 获取联系人数据
			getContactsPerson(String.valueOf(UserManager.getInstance()
					.getUser().getUid()), getContactsJSON(mContactsNumber));
		} else {
			promptTextView.setVisibility(View.VISIBLE);
			promptTextView.setText(R.string.contact_none);
		}
	}

	// ////////////////////private method///////////////////////////////////
	/**
	 * 初始化
	 * */
	private void init() {
		setBarText(getString(R.string.contact_add_friend));
		// 显示头像的配置
		imgLoader = ImageLoader.getInstance();
		// 显示图片的配置
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.loading_default)
				.showImageOnFail(R.drawable.icon).cacheInMemory(true)
				.cacheOnDisk(true).bitmapConfig(Bitmap.Config.RGB_565).build();
	}

	/**
	 * 数据绑定初始化
	 * */
	private void listviewSet() {
		// 设置刷新模式
		contactsListView.setMode(Mode.DISABLED);
		contactsListView.setClickable(true);
		contactsAdapter = new HelloHaAdapter<PersonModel>(
				ContactsUserActivity.this, R.layout.add_contacts_item_layout,
				dataList) {

			@Override
			protected void convert(final HelloHaBaseAdapterHelper helper,
					final PersonModel item) {
				final PersonModel currentPerson = item;
				// 绑定头像图片
				ImageView headImageView = helper.getView(R.id.iv_contacts_head);
				if (null != item.getHeadSubImage()
						&& item.getHeadSubImage().length() > 0) {
					imgLoader.displayImage(item.getHeadSubImage(),
							headImageView, options);
				} else {
					headImageView.setImageResource(R.drawable.icon);
				}
				// 绑定昵称
				helper.setText(R.id.tv_contact_user_name, item.getUserName());
				if (item.getUserName() == null || item.getUserName().length() < 1) {
					helper.setText(R.id.tv_contact_user_name, getString(R.string.personal_none));	
				}
				
				// 通讯录名称
				String contactName = "";
				if (mContactsNumber.contains(item.getPhoneNumber())) {
					int index = mContactsNumber.indexOf(item.getPhoneNumber());
					contactName = mContactsName.get(index);
				}

				helper.setText(R.id.tv_contacts_name, contactName);
				helper.setOnClickListener(R.id.layout_contacts_root_view,
						new OnClickListener() {

							@Override
							public void onClick(View v) {
								// 跳转至用户的主页
								Intent intentUsrMain = new Intent(
										ContactsUserActivity.this,
										OtherPersonalActivity.class);
								intentUsrMain.putExtra(
										OtherPersonalActivity.INTENT_KEY,
										KHUtils.stringToInt(currentPerson
												.getUerId()));
								startActivityWithRight(intentUsrMain);
							}
						});

				Button addButton = helper.getView(R.id.btn_contacts_add);
				
				User imUser = ((KHHXSDKHelper) KHHXSDKHelper.getInstance())
						.getContactList().get(KHConst.KH + item.getUerId());
				
				if (null != imUser) {
					addButton.setText(R.string.contact_added);
					addButton.setBackgroundResource(R.color.main_gary);
					addButton.setTextColor(getResources().getColorStateList(
							R.color.main_white));
					addButton.setEnabled(false);
				} else {
					addButton.setText(R.string.contact_add);
					addButton.setBackgroundResource(R.color.main_yellow);
					addButton.setTextColor(getResources().getColorStateList(
							R.color.main_brown));
					addButton.setEnabled(true);
				}

				// 点击添加按钮
				helper.setOnClickListener(R.id.btn_contacts_add,
						new OnClickListener() {

							@Override
							public void onClick(View v) {
								// 点击添加按钮
								addContact(item.getUerId());
							}
						});
			}
		};

		/**
		 * 刷新监听
		 * */
		contactsListView
				.setOnRefreshListener(new OnRefreshListener2<ListView>() {

					@Override
					public void onPullDownToRefresh(
							PullToRefreshBase<ListView> refreshView) {
						contactsListView.postDelayed(new Runnable() {
							@Override
							public void run() {
								contactsListView.onRefreshComplete();
							}
						}, 1);
					}

					@Override
					public void onPullUpToRefresh(
							PullToRefreshBase<ListView> refreshView) {
						contactsListView.postDelayed(new Runnable() {
							@Override
							public void run() {
								contactsListView.onRefreshComplete();
							}
						}, 1);
					}
				});

		// 快速滑动时不加载图片
		contactsListView.setAdapter(contactsAdapter);
		contactsListView.setClickable(true);
	}

	/**
	 * 电话数据转换为jsonObject
	 * */
	private String getContactsJSON(ArrayList<String> numberList) {
		JSONArray array = new JSONArray();

		for (String phoneNum : numberList) {
			array.add(phoneNum);
		}
		return array.toJSONString();
	}

	/**
	 * 得到手机通讯录联系人信息
	 **/
	private void getPhoneContacts() {
		ContentResolver resolver = ContactsUserActivity.this
				.getContentResolver();
		// 获取手机联系人
		Cursor phoneCursor = resolver.query(Phone.CONTENT_URI,
				PHONES_PROJECTION, null, null, null);
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
				if (TextUtils.isEmpty(phoneNumber)) {
					continue;
				}
				String contactName = phoneCursor
						.getString(PHONES_DISPLAY_NAME_INDEX);
				if (phoneNumber.length() == 11) {
					mContactsName.add(contactName);
					mContactsNumber.add(phoneNumber);
				}
			}
			phoneCursor.close();
		}
	}

	/**
	 * 得到手机SIM卡联系人人信息
	 **/
	private void getSIMContacts() {
		ContentResolver resolver = ContactsUserActivity.this
				.getContentResolver();
		Uri uri = Uri.parse("content://icc/adn");
		Cursor phoneCursor = resolver.query(uri, PHONES_PROJECTION, null, null,
				null);

		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {
				String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
				if (TextUtils.isEmpty(phoneNumber)) {
					continue;
				}
				String contactName = phoneCursor
						.getString(PHONES_DISPLAY_NAME_INDEX);
				if (phoneNumber.length() == 11) {
					mContactsName.add(contactName);
					mContactsNumber.add(phoneNumber);
				}
			}
			phoneCursor.close();
		}
	}

	/**
	 * sdcard是否可读写
	 * */
	private boolean IsCanUseSdCard() {
		try {
			return Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * sim卡是否可读
	 **/
	private boolean isCanUseSim() {
		try {
			TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

			return TelephonyManager.SIM_STATE_READY == mgr.getSimState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 获取通讯录好友信息
	 * */
	private void getContactsPerson(String userId, String contact) {
		// 网络请求
		showLoading(getString(R.string.downloading), true);
		RequestParams params = new RequestParams();
		params.addBodyParameter("user_id", userId);
		params.addBodyParameter("contact", contact);
		HttpManager.post(KHConst.GET_CONTACT_USER, params,
				new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

					@Override
					public void onSuccess(JSONObject jsonResponse, String flag) {
						super.onSuccess(jsonResponse, flag);
						int status = jsonResponse
								.getInteger(KHConst.HTTP_STATUS);
						switch (status) {
						case KHConst.STATUS_SUCCESS:
							hideLoading();
							JSONObject jResult = jsonResponse
									.getJSONObject(KHConst.HTTP_RESULT);
							// 获取数据列表
							@SuppressWarnings("unchecked")
							List<JSONObject> JPersonList = (List<JSONObject>) jResult
									.get("list");
							jsonToPersonData(JPersonList);
							break;

						case KHConst.STATUS_FAIL:
							hideLoading();
							ToastUtil.show(ContactsUserActivity.this,
									jsonResponse
											.getString(KHConst.HTTP_MESSAGE));
						}
					}

					@Override
					public void onFailure(HttpException arg0, String arg1,
							String flag) {
						super.onFailure(arg0, arg1, flag);
						hideLoading();
						promptTextView.setVisibility(View.VISIBLE);
						promptTextView.setText(R.string.downloading_fail);
						ToastUtil.show(ContactsUserActivity.this, R.string.net_error);
					}

				}, null));
	}

	/**
	 * 数据解析
	 * */
	private void jsonToPersonData(List<JSONObject> jPersonList) {

		dataList.clear();
		for (JSONObject likeObj : jPersonList) {
			PersonModel tempPerson = new PersonModel();
			tempPerson.setContentWithJson(likeObj);
			dataList.add(tempPerson);
		}
		contactsAdapter.addAll(dataList);

		if (contactsAdapter.getCount() == 0) {
			promptTextView.setVisibility(View.VISIBLE);
			promptTextView.setText(R.string.contact_none);
		} else {
			promptTextView.setVisibility(View.GONE);
		}
	}

	/**
	 * 加载图片时的回调函数
	 * */
	public class NewsBitmapLoadCallBack extends
			DefaultBitmapLoadCallBack<ImageView> {
		private final ImageView iView;

		public NewsBitmapLoadCallBack() {
			this.iView = null;
		}

		// 开始加载
		@Override
		public void onLoadStarted(ImageView container, String uri,
				BitmapDisplayConfig config) {
			//
			super.onLoadStarted(container, uri, config);
		}

		// 加载过程中
		@Override
		public void onLoading(ImageView container, String uri,
				BitmapDisplayConfig config, long total, long current) {
		}

		// 加载完成时
		@Override
		public void onLoadCompleted(ImageView container, String uri,
				Bitmap bitmap, BitmapDisplayConfig config, BitmapLoadFrom from) {
			container.setImageBitmap(bitmap);
		}
	}

	// 添加好友
	/**
	 * 添加contact
	 * 
	 * @param view
	 */
	public void addContact(String uid) {

		final String targetIMID = KHConst.KH + uid;
		if (KHUtils.selfCommonIMID().equals(targetIMID)) {
			String str = getString(R.string.not_add_myself);
			startActivity(new Intent(this, AlertDialog.class).putExtra("msg",
					str));
			return;
		}
		if (((KHHXSDKHelper) HXSDKHelper.getInstance()).getContactList()
				.containsKey(targetIMID)) {
			// 提示已在好友列表中，无需添加
			if (EMContactManager.getInstance().getBlackListUsernames()
					.contains(targetIMID)) {
				startActivity(new Intent(this, AlertDialog.class).putExtra(
						"msg", R.string.im_in_black_list));
				return;
			}
			String strin = getString(R.string.This_user_is_already_your_friend);
			startActivity(new Intent(this, AlertDialog.class).putExtra("msg",
					strin));
			return;
		}

		final ProgressDialog progressDialog = new ProgressDialog(this);
		String stri = getResources().getString(R.string.Is_sending_a_request);
		progressDialog.setMessage(stri);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.show();

		new Thread(new Runnable() {
			public void run() {

				try {
					// demo写死了个reason，实际应该让用户手动填入
					// 先就用DEMO这个死的
					String s = getResources().getString(R.string.Add_a_friend);
					EMContactManager.getInstance().addContact(targetIMID, s);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							String s1 = getResources().getString(
									R.string.send_successful);
							Toast.makeText(getApplicationContext(), s1, 1)
									.show();
						}
					});
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							String s2 = getResources().getString(
									R.string.Request_add_buddy_failure);
							Toast.makeText(getApplicationContext(),
									s2 + e.getMessage(), 1).show();
						}
					});
				}
			}
		}).start();
	}
}

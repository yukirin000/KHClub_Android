package com.app.khclub.news.ui.utils;

import android.content.Context;

import com.alibaba.fastjson.JSONObject;
import com.app.khclub.R;
import com.app.khclub.R.string;
import com.app.khclub.base.adapter.HelloHaAdapter;
import com.app.khclub.base.helper.JsonRequestCallBack;
import com.app.khclub.base.helper.LoadDataHandler;
import com.app.khclub.base.manager.HttpManager;
import com.app.khclub.base.manager.UserManager;
import com.app.khclub.base.model.UserModel;
import com.app.khclub.base.utils.KHConst;
import com.app.khclub.base.utils.LogUtils;
import com.app.khclub.base.utils.ToastUtil;
import com.app.khclub.news.ui.model.CommentModel;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;

public class NewsOperate {
	// 对动态的操作类型
	public final static int OP_Type_Delete_News = 0;
	public final static int OP_Type_Add_Comment = 1;
	public final static int OP_Type_Delete_Comment = 2;
	public final static int OP_Type_Add_Reply_Comment = 3;
	public final static int OP_Type_Delete_Reply_Comment = 4;
	public final static int OP_Type_Like = 5;
	public final static int OP_Type_Like_cancel = 6;

	// 记录上次操作
	private int lastOperateType = -1;
	// 上下文
	private Context mContext;
	// 回调接口
	private OperateCallBack callInterface;
	// 点赞回调接口
	private LikeCallBack likeCallInterface;
	// 记录动态点赞的操作位置
	private int lastPostion = 0;
	// 是否正在传输数据
	private boolean isUploadData = false;

	public NewsOperate(Context context) {
		this.mContext = context;
	}

	// 设置回调
	public void setOperateListener(OperateCallBack callInterface) {
		this.callInterface = callInterface;
	}

	// 设置点赞回调
	public void setLikeListener(LikeCallBack callInterface) {
		this.likeCallInterface = callInterface;
	}

	/**
	 * 删除动态操作
	 * */
	public void deleteNews(String newsId) {
		lastOperateType = OP_Type_Delete_News;
		callInterface.onStart(OP_Type_Delete_News);
		RequestParams params = new RequestParams();
		params.addBodyParameter("news_id", newsId);

		HttpManager.post(KHConst.DELETE_NEWS, params,
				new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

					@Override
					public void onSuccess(JSONObject jsonResponse, String flag) {
						super.onSuccess(jsonResponse, flag);
						int status = jsonResponse
								.getInteger(KHConst.HTTP_STATUS);
						if (status == KHConst.STATUS_SUCCESS) {
							callInterface.onFinish(OP_Type_Delete_News, true,
									null);
						}

						if (status == KHConst.STATUS_FAIL) {
							callInterface.onFinish(OP_Type_Delete_News, false,
									null);
							ToastUtil.show(mContext, mContext
									.getString(R.string.network_unavailable));
						}
					}

					@Override
					public void onFailure(HttpException arg0, String arg1,
							String flag) {
						super.onFailure(arg0, arg1, flag);
						callInterface
								.onFinish(OP_Type_Delete_News, false, null);
						ToastUtil.show(mContext, mContext
								.getString(R.string.network_unavailable));
					}
				}, null));
	}

	/**
	 * 发布评论
	 * 
	 * 参数依次是
	 * 
	 * @param user
	 *            当前操作的用户
	 * @param newsID
	 *            操作动态的id
	 * @param content
	 *            发布评论的内容
	 * @param targetID
	 *            目标用户的id（被回复者，评论时可省略）
	 * */

	public void publishComment(final UserModel user, final String... parameter) {

		lastOperateType = OP_Type_Add_Comment;
		callInterface.onStart(OP_Type_Add_Comment);
		RequestParams params = new RequestParams();
		params.addBodyParameter("user_id", String.valueOf(user.getUid()));
		params.addBodyParameter("news_id", parameter[0]);
		params.addBodyParameter("comment_content", parameter[1]);

		if (parameter.length >= 3) {
			params.addBodyParameter("target_id", parameter[2]);
		}
		if (parameter.length >= 4) {
			params.addBodyParameter("target_name", parameter[3]);
		}

		HttpManager.post(KHConst.SEND_COMMENT, params,
				new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

					@Override
					public void onSuccess(JSONObject jsonResponse, String flag) {
						super.onSuccess(jsonResponse, flag);
						int status = jsonResponse
								.getInteger(KHConst.HTTP_STATUS);
						if (status == KHConst.STATUS_SUCCESS) {
							// 获取评论成功的返回值
							JSONObject JResult = jsonResponse
									.getJSONObject(KHConst.HTTP_RESULT);
							CommentModel temMode = new CommentModel();
							temMode.setContentWithJson(JResult);
							temMode.setPublishName(user.getName());
							temMode.setHeadSubImage(KHConst.ATTACHMENT_ADDR
									+ user.getHead_sub_image());
							temMode.setUserJob(user.getJob());
							if (parameter.length >= 3) {
								temMode.setTargetUserId(parameter[2]);
							} else {
								temMode.setTargetUserId("");
							}
							if (parameter.length >= 4) {
								temMode.setTargetUserName(parameter[3]);
							} else {
								temMode.setTargetUserName("");
							}

							callInterface.onFinish(OP_Type_Add_Comment, true,
									temMode);
						}

						if (status == KHConst.STATUS_FAIL) {
							callInterface.onFinish(OP_Type_Add_Comment, false,
									null);
							if (jsonResponse.getInteger(KHConst.HTTP_RESULT) == 1) {
								//没关注圈子
								ToastUtil.show(mContext, mContext.getString(R.string.circle_no_follow));
							}else {
								ToastUtil.show(mContext, mContext.getString(R.string.network_unavailable));								
							}
						}
					}

					@Override
					public void onFailure(HttpException arg0, String arg1,
							String flag) {
						super.onFailure(arg0, arg1, flag);
						callInterface
								.onFinish(OP_Type_Add_Comment, false, null);
						ToastUtil.show(mContext, mContext
								.getString(R.string.network_unavailable));
					}
				}, null));
	}

	/**
	 * 删除评论
	 * 
	 * @param CID
	 *            评论的id
	 * @param newsID
	 *            动态的id
	 * */
	public void deleteComment(String CID, String newsID) {
		lastOperateType = OP_Type_Delete_Comment;
		callInterface.onStart(OP_Type_Delete_Comment);
		RequestParams params = new RequestParams();
		params.addBodyParameter("cid", CID);
		params.addBodyParameter("news_id", newsID);

		HttpManager.post(KHConst.DELETE_COMMENT, params,
				new JsonRequestCallBack<String>(new LoadDataHandler<String>() {

					@Override
					public void onSuccess(JSONObject jsonResponse, String flag) {
						super.onSuccess(jsonResponse, flag);
						int status = jsonResponse
								.getInteger(KHConst.HTTP_STATUS);
						if (status == KHConst.STATUS_SUCCESS) {
							callInterface.onFinish(OP_Type_Delete_Comment,
									true, null);
						}

						if (status == KHConst.STATUS_FAIL) {
							callInterface.onFinish(OP_Type_Delete_Comment,
									false, null);
							ToastUtil.show(mContext, mContext
									.getString(R.string.network_unavailable));
						}
					}

					@Override
					public void onFailure(HttpException arg0, String arg1,
							String flag) {
						super.onFailure(arg0, arg1, flag);
						callInterface.onFinish(OP_Type_Delete_Comment, false,
								null);
					}
				}, null));
	}

	/**
	 * 点赞操作网络请求
	 * 
	 * @param newsId
	 *            点赞操作的动态id
	 * @param isLike
	 *            是不是点赞操作
	 */
	public void uploadLikeOperate(String newsId, boolean isLike) {
		if (!isUploadData) {
			isUploadData = true;
			if (isLike) {
				likeCallInterface.onLikeStart(true);
			} else {
				likeCallInterface.onLikeStart(false);
			}
			// 参数设置
			RequestParams params = new RequestParams();
			params.addBodyParameter("news_id", newsId);
			if (isLike) {
				params.addBodyParameter("isLike", "1");
			} else {
				params.addBodyParameter("isLike", "0");
			}
			params.addBodyParameter("user_id", String.valueOf(UserManager
					.getInstance().getUser().getUid()));
			params.addBodyParameter("is_second", "0");
			HttpManager.post(KHConst.LIKE_OR_CANCEL, params,
					new JsonRequestCallBack<String>(
							new LoadDataHandler<String>() {

								@Override
								public void onSuccess(JSONObject jsonResponse,
										String flag) {
									super.onSuccess(jsonResponse, flag);
									int status = jsonResponse
											.getInteger(KHConst.HTTP_STATUS);
									if (status == KHConst.STATUS_SUCCESS) {
										// 点赞成功
										isUploadData = false;
									}

									if (status == KHConst.STATUS_FAIL) {
										if (OP_Type_Like == lastOperateType) {
											likeCallInterface.onLikeFail(true);
										} else {
											likeCallInterface.onLikeFail(false);
										}
										ToastUtil
												.show(mContext,
														mContext.getString(R.string.network_unavailable));
										isUploadData = false;
									}
								}

								@Override
								public void onFailure(HttpException arg0,
										String arg1, String flag) {
									super.onFailure(arg0, arg1, flag);
									if (OP_Type_Like == lastOperateType) {
										likeCallInterface.onLikeFail(true);
									} else {
										likeCallInterface.onLikeFail(false);
									}
									isUploadData = false;
								}
							}, null));
		}
	}

	/**
	 * 操作回调接口
	 **/
	public interface OperateCallBack {
		public void onStart(int operateType);

		public void onFinish(int operateType, boolean isSucceed,
				Object resultValue);
	}

	/**
	 * 点赞回调接口
	 * */
	public interface LikeCallBack {
		public void onLikeStart(boolean isLike);

		public void onLikeFail(boolean isLike);
	}
}

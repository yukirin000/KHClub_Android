/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.app.khclub.contact.ui.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;

import com.app.khclub.R;
import com.app.khclub.base.easeim.Constant;
import com.app.khclub.base.easeim.KHHXSDKHelper;
import com.app.khclub.base.easeim.activity.BaseActivity;
import com.app.khclub.base.easeim.activity.ChatActivity;
import com.app.khclub.base.easeim.adapter.ContactAdapter;
import com.app.khclub.base.easeim.applib.controller.HXSDKHelper;
import com.app.khclub.base.easeim.domain.User;
import com.app.khclub.base.easeim.widget.Sidebar;
import com.easemob.chat.EMConversation.EMConversationType;

//分析用contact
public class ShareContactsActivity extends BaseActivity {
	
	public static String INTENT_CARD_KEY = "cardKey";
	private ListView listView;
	private PickContactAdapter contactAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_pick_contacts);
		
		// 获取好友列表
		final List<User> alluserList = new ArrayList<User>();
		for (User user : ((KHHXSDKHelper)HXSDKHelper.getInstance()).getContactList().values()) {
			if (!user.getUsername().equals(Constant.NEW_FRIENDS_USERNAME) & !user.getUsername().equals(Constant.GROUP_USERNAME) & !user.getUsername().equals(Constant.CHAT_ROOM) & !user.getUsername().equals(Constant.CHAT_ROBOT))
				alluserList.add(user);
		}
		
		// 对list进行排序
		Collections.sort(alluserList, new Comparator<User>() {
			@Override
			public int compare(User lhs, User rhs) {
				return (lhs.getUsername().compareTo(rhs.getUsername()));

			}
		});

		listView = (ListView) findViewById(R.id.list);
		contactAdapter = new PickContactAdapter(this, R.layout.row_contact_with_checkbox, alluserList);
		listView.setAdapter(contactAdapter);
		((Sidebar) findViewById(R.id.sidebar)).setListView(listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent lastIntent = getIntent();
				if (lastIntent.hasExtra(INTENT_CARD_KEY)) {
					Intent intent = new Intent(ShareContactsActivity.this, ChatActivity.class);
					intent.putExtra("userId", alluserList.get(position).getUsername());
					intent.putExtra(INTENT_CARD_KEY, lastIntent.getStringExtra(INTENT_CARD_KEY));
					startActivity(intent);	
					finish();
				}
			}
		});
	}

	/**
	 * 确认选择的members
	 * 
	 * @param v
	 */
	public void save(View v) {
		setResult(RESULT_OK, new Intent().putExtra("newmembers", getToBeAddMembers().toArray(new String[0])));
		finish();
	}

	/**
	 * 获取要被添加的成员
	 * 
	 * @return
	 */
	private List<String> getToBeAddMembers() {
		List<String> members = new ArrayList<String>();
		int length = contactAdapter.isCheckedArray.length;
		for (int i = 0; i < length; i++) {
			String username = contactAdapter.getItem(i).getUsername();
			if (contactAdapter.isCheckedArray[i]) {
				members.add(username);
			}
		}

		return members;
	}

	/**
	 * adapter
	 */
	private class PickContactAdapter extends ContactAdapter {

		private boolean[] isCheckedArray;

		public PickContactAdapter(Context context, int resource, List<User> users) {
			super(context, resource, users);
			isCheckedArray = new boolean[users.size()];
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			// 选择框checkbox
			final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			checkBox.setVisibility(View.INVISIBLE);
			return view;
		}
	}

	public void back(View view){
		finish();
	}
	
}

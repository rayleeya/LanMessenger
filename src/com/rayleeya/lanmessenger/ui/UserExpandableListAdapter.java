package com.rayleeya.lanmessenger.ui;

import java.util.ArrayList;
import java.util.List;

import com.rayleeya.lanmessenger.model.Group;
import com.rayleeya.lanmessenger.model.User;
import com.rayleeya.lanmessenger.util.Utils;
import com.rayleeya.lanmessenger.R;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class UserExpandableListAdapter extends BaseExpandableListAdapter 
									   implements ExpandableListView.OnChildClickListener, 
									              ExpandableListView.OnGroupClickListener,
									   			  ExpandableListView.OnGroupCollapseListener,
									   			  ExpandableListView.OnGroupExpandListener{

	private List<Group> mGroups;
	private Context mContext;
	private LayoutInflater mInflater;
	
	public UserExpandableListAdapter(Context cxt) {
		mContext = cxt;
		mInflater = (LayoutInflater)cxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGroups = new ArrayList<Group>();
	}
	
	public void setContent(List<Group> content) {
		if (content == null) content = new ArrayList<Group>();
		mGroups = content;
	}
	
    //-------- For Users --------
	@Override
	public User getChild(int groupPosition, int childPosition) {
		Group group = mGroups.get(groupPosition);
		User user = group.getUser(childPosition);
		return user;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		Group group = mGroups.get(groupPosition);
		User user = group.getUser(childPosition);
		
		View view = null;
		if (convertView == null) {
			view = mInflater.inflate(R.layout.simple_expandable_list_item_2, null);
		} else {
			view = convertView;
		}
		
		TextView tv = (TextView) view.findViewById(R.id.username);
		tv.setText(user.getUsername());
		
		tv = (TextView) view.findViewById(R.id.userip);
		tv.setText(user.getIp());
		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		Group group = mGroups.get(groupPosition);
		return group.getUsers().size();
	}

	//-------- For Groups --------
	@Override
	public Group getGroup(int groupPosition) {
		Group group = mGroups.get(groupPosition);
		return group;
	}

	@Override
	public int getGroupCount() {
		return mGroups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		Group group = mGroups.get(groupPosition);
		View view = null;
		if (convertView == null) {
			view = mInflater.inflate(R.layout.simple_expandable_list_item_1, null);
		} else {
			view = convertView;
		}
		
		TextView tv = (TextView) view.findViewById(R.id.groupname);
		tv.setText(group.getName());
		return view;
	}

	@Override
	public boolean hasStableIds() {
		
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		Group group = mGroups.get(groupPosition);
		return group.getUser(childPosition).isSelected();
	}

	//---------- implements for ExpandableListView's listeners ----------
	@Override
	public void onGroupExpand(int groupPosition) {
		
		
	}

	@Override
	public void onGroupCollapse(int groupPosition) {
		
		
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		Intent i = new Intent(mContext, ChatRoomActivity.class);
		i.putExtra(Utils.EXTRA_GROUP_ID, groupPosition);
		i.putExtra(Utils.EXTRA_USER_ID, childPosition);
		mContext.startActivity(i);
		return true;
	}

	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		return false;
	}

}

package com.keenon.peanut.sample.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.keenon.peanut.sample.R;

import java.util.List;

public class UserAdapter extends BaseAdapter {
    private Context context;
    private List<User> userList;
    private LayoutInflater inflater;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return userList != null ? userList.size() : 0;
    }

    @Override
    public User getItem(int position) {
        return userList != null ? userList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user, parent, false);
            holder = new ViewHolder();
            holder.tvUserName = convertView.findViewById(R.id.tv_user_name);
            holder.tvUserId = convertView.findViewById(R.id.tv_user_id);
            holder.tvUserEmail = convertView.findViewById(R.id.tv_user_email);
            holder.tvUserAddress = convertView.findViewById(R.id.tv_user_address);
            holder.tvUserCompany = convertView.findViewById(R.id.tv_user_company);
            holder.tvUserPhone = convertView.findViewById(R.id.tv_user_phone);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = getItem(position);
        if (user != null) {
            holder.tvUserName.setText(user.getName());
            holder.tvUserId.setText("#" + user.getId());
            holder.tvUserEmail.setText(user.getEmail());
            holder.tvUserPhone.setText(user.getPhone());

            // Cập nhật địa chỉ
            holder.tvUserAddress.setText(user.getAddress() != null ?
                    user.getAddress().getFullAddress() : "Không có địa chỉ");

            // Cập nhật thông tin công ty
            holder.tvUserCompany.setText(user.getCompany() != null ?
                    user.getCompany().getName() : "Không có thông tin công ty");
        }

        return convertView;
    }

    public void updateData(List<User> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView tvUserName;
        TextView tvUserId;
        TextView tvUserEmail;
        TextView tvUserAddress;
        TextView tvUserCompany;
        TextView tvUserPhone;
    }
} 
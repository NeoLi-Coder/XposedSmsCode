package com.tianma.xsmscode.ui.record;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.tianma8023.xposed.smscode.R;
import com.tianma.xsmscode.common.adapter.ItemCallback;
import com.tianma.xsmscode.common.adapter.ItemChildCallback;
import com.tianma.xsmscode.data.db.entity.SmsMsg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class CodeRecordAdapter extends RecyclerView.Adapter<CodeRecordAdapter.VH> {

    private Context mContext;
    private List<RecordItem> mRecords;
    private final List<RecordItem> visibleRecords = new ArrayList<>();
    private String query = "";

    private SimpleDateFormat mFormat;

    private ItemCallback<RecordItem> mItemCallback;
    private ItemChildCallback<RecordItem> mItemChildCallback;

    // normal mode
    static final int RECORD_MODE_NORMAL = 0;
    // edit mode
    static final int RECORD_MODE_EDIT = 1;

    @IntDef({RECORD_MODE_NORMAL, RECORD_MODE_EDIT})
    @interface RecordMode {
    }


    // current mode
    @RecordMode
    private int mMode = RECORD_MODE_NORMAL;

    CodeRecordAdapter(Context context, List<RecordItem> records) {
        mContext = context;
        mRecords = records;
        visibleRecords.addAll(records);

        mFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    public void setItemCallback(ItemCallback<RecordItem> itemCallback) {
        mItemCallback = itemCallback;
    }

    public void setItemChildCallback(ItemChildCallback<RecordItem> itemChildCallback) {
        mItemChildCallback = itemChildCallback;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(mContext).inflate(R.layout.item_code_record, parent, false);
        return new VH(rootView);
    }

    @Override
    public int getItemCount() {
        return visibleRecords.size();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final RecordItem data = getItemAt(position);
        holder.bindData(data, position);
        holder.bindListener(data, position);
    }

    class VH extends RecyclerView.ViewHolder {

        @BindView(R.id.company_text_view)
        TextView mCompanyTv;

        @BindView(R.id.smscode_text_view)
        TextView mSmsCodeTv;

        @BindView(R.id.date_text_view)
        TextView mDateTv;

        @BindView(R.id.record_details_view)
        TextView mDetailsView;

        @BindView(R.id.checkbox)
        AppCompatCheckBox mCheckBox;

        VH(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindData(RecordItem data, int position) {
            SmsMsg smsMsg = data.getSmsMsg();
            mCompanyTv.setText(smsMsg.getCompany());
            String company = smsMsg.getCompany();
            if (company != null && company.trim().length() != 0) {
                mCompanyTv.setText(company);
            } else {
                mCompanyTv.setText(smsMsg.getSender());
            }
            mSmsCodeTv.setText(smsMsg.getSmsCode());
            mDateTv.setText(mFormat.format(new Date(smsMsg.getDate())));

            if (mMode == RECORD_MODE_NORMAL) {
                mCheckBox.setVisibility(View.GONE);
            } else {
                mCheckBox.setVisibility(View.VISIBLE);
                mCheckBox.setChecked(data.isSelected());
            }

            mDetailsView.setVisibility(TextUtils.isEmpty(smsMsg.getBody()) ? View.GONE : View.VISIBLE);
        }

        void bindListener(final RecordItem data, final int position) {
            if (mItemCallback != null) {
                itemView.setOnClickListener(v -> mItemCallback.onItemClicked(itemView, data, position));

                itemView.setOnLongClickListener(v -> mItemCallback.onItemLongClicked(itemView, data, position));
            }

            if (mItemChildCallback != null) {
                mDetailsView.setOnClickListener(v -> {
                    mItemChildCallback.onItemChildClicked(mDetailsView, data, position);
                });

                mCheckBox.setOnClickListener(v -> {
                    mItemChildCallback.onItemChildClicked(mCheckBox, data, position);
                });
            }
        }
    }

    private RecordItem getItemAt(int position) {
        return visibleRecords.get(position);
    }

    public void setItemSelected(int position, boolean selected) {
        RecordItem recordItem = getItemAt(position);
        recordItem.setSelected(selected);
        notifyDataSetChanged();
    }

    public boolean isItemSelected(int position) {
        return getItemAt(position).isSelected();
    }

    public void setAllSelected(boolean selected) {
        for (int i = 0; i < getItemCount(); i++) {
            getItemAt(i).setSelected(selected);
        }
        notifyDataSetChanged();
    }


    public boolean isAllSelected() {
        boolean allSelected = true;
        for (int i = 0; i < getItemCount(); i++) {
            if (!isItemSelected(i)) {
                allSelected = false;
                break;
            }
        }
        return allSelected;
    }

    public List<SmsMsg> removeSelectedItems() {
        List<RecordItem> recordsToRemove = new ArrayList<>();
        List<SmsMsg> messagesToRemove = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            RecordItem item = getItemAt(i);
            if (item.isSelected()) {
                recordsToRemove.add(item);
                messagesToRemove.add(item.getSmsMsg());
            }
        }
        mRecords.removeAll(recordsToRemove);
        filterRecords();
        return messagesToRemove;
    }

    public void addItems(List<SmsMsg> smsMsgList) {
        List<RecordItem> itemsToAdd = new ArrayList<>();
        for (SmsMsg msg : smsMsgList) {
            RecordItem item = new RecordItem(msg);
            if (!mRecords.contains(item)) {
                itemsToAdd.add(item);
            }
        }

        if (!itemsToAdd.isEmpty()) {
            mRecords.addAll(itemsToAdd);
            Collections.sort(mRecords, (o1, o2) -> {
                long date1 = o1.getSmsMsg().getDate();
                long date2 = o2.getSmsMsg().getDate();
                return Long.compare(date2, date1);
            });
            filterRecords();
        }
    }

    public void replaceItems(List<SmsMsg> messages) {
        mRecords.clear();
        addItems(messages);
        filterRecords();
    }

    public void setQuery(String value) {
        query = value.trim().toLowerCase(Locale.ROOT);
        // 搜索范围改变时退出多选，防止删除不可见的已选记录。
        for (RecordItem item : mRecords) item.setSelected(false);
        mMode = RECORD_MODE_NORMAL;
        filterRecords();
    }

    public boolean hasQuery() { return !query.isEmpty(); }

    private void filterRecords() {
        visibleRecords.clear();
        for (RecordItem item : mRecords) {
            SmsMsg msg = item.getSmsMsg();
            if (query.isEmpty() || containsQuery(msg.getSender()) || containsQuery(msg.getCompany())
                    || containsQuery(msg.getSmsCode()) || containsQuery(msg.getBody())) {
                visibleRecords.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private boolean containsQuery(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    void setMode(@RecordMode int mode) {
        if (mMode != mode) {
            mMode = mode;
            notifyDataSetChanged();
        }
    }

    @RecordMode
    int getMode() {
        return mMode;
    }

}

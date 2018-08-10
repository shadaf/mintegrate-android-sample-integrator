package com.mintwireless.mintegrate.console.ui;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mintwireless.mintegrate.console.R;
import com.mintwireless.mintegrate.console.TransactionAdapter;
import com.mintwireless.mintegrate.console.utils.LoadingHelper;
import com.mintwireless.mintegrate.console.utils.Logger;
import com.mintwireless.mintegrate.core.ResponseCallback;
import com.mintwireless.mintegrate.core.Session;
import com.mintwireless.mintegrate.core.exceptions.MintegrateError;
import com.mintwireless.mintegrate.core.requests.GetTransactionsRequest;
import com.mintwireless.mintegrate.core.responses.BaseResponse;
import com.mintwireless.mintegrate.core.responses.GetTransactionsResponse;
import com.mintwireless.mintegrate.core.responses.SubmitPaymentResponse;
import com.mintwireless.mintegrate.sdk.Mintegrate;

/**
 * Created by Jialian on 6/05/16.
 */
public class TransactionsActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener,
        TransactionAdapter.ItemOnClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private Session mSession;
    private String authToken;
    private TransactionAdapter transactionAdapter;
    private Toolbar toolbar;
    private LOADING_ACTION mCurrentAction = LOADING_ACTION.NONE;
    private MenuItem action_load_more;

    public enum LOADING_ACTION {
        NONE,
        REFRESH,
        LOAD_MORE
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        authToken = getIntent().getStringExtra("authtoken");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresher);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.transactions_list);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SpaceItemDecoration(30, 30, 30, 30));
        transactionAdapter = new TransactionAdapter(this);
        recyclerView.setAdapter(transactionAdapter);
        mCurrentAction = LOADING_ACTION.REFRESH;
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                mCurrentAction = LOADING_ACTION.REFRESH;
                action_load_more.setEnabled(false);
                fetchTransaction();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_menu, menu);
        this.action_load_more = menu.findItem(R.id.action_load_more);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_load_more) {
            mCurrentAction = LOADING_ACTION.LOAD_MORE;
            invalidateOptionsMenu();
            LoadingHelper.showLoading(getSupportFragmentManager());
            loadMoreTransactions();
        } else if (itemId == android.R.id.home) {
            closeSession();
            finish();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeSession();
        finish();
    }

    private void loadMoreTransactions() {
        try {

            GetTransactionsRequest getTransactionsRequest = new GetTransactionsRequest();
            getTransactionsRequest.setAuthToken(authToken);
            getTransactionsRequest.setLatest(false); // must set to false to load more transactions

            // set transaction request ID to load more transactions which are made before this transaction
            getTransactionsRequest.setTransactionsRequestId(transactionAdapter.getLastTransactionRequestID());
            mSession = Mintegrate.getTransactions(getTransactionsRequest, new ResponseCallback<GetTransactionsResponse>() {
                @Override
                public void onCompletion(Session session, GetTransactionsResponse getTransactionsResponse) {
                 onCompletion(session, getTransactionsResponse);
                }

                @Override
                public void onError(Session session, MintegrateError.Error error) {
                        onError(session, error);
                }
            });
            mSession.next(); // start the session

        } catch (Exception e) {
        }
    }

    private void setRefreshing(boolean isRefreshing) {
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(isRefreshing);
    }

    private void closeSession() {
        if (mSession != null)
            mSession.close();
    }

    public void onError(Session session, MintegrateError.Error error) {
        Logger.logInfo(TransactionsActivity.class, "onError");
        closeSession();
        restore();
    }

    public void onCompletion(Session session, BaseResponse baseResponse) {
        Logger.logInfo(TransactionsActivity.class, "onCompletion");
        closeSession();
        restore();
        if (baseResponse instanceof GetTransactionsResponse) {
            processTransactionsList((GetTransactionsResponse) baseResponse);
        } else {
            SubmitPaymentResponse submitPaymentResponse = (SubmitPaymentResponse) baseResponse;
            SubmitPaymentResponse.Status status = submitPaymentResponse.getTransactionStatus();
            String message = null;
            switch (status) {
                case APPROVED:
                    message = "Transaction Approved";
                    break;
                case DECLINED:
                    message = "Transaction Declined";
                    break;
                case CANCELLED:
                    message = "Transaction Cancelled";
            }

            Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private void restore() {
        if (mCurrentAction == LOADING_ACTION.REFRESH) {
            action_load_more.setEnabled(true);
            setRefreshing(false);
        } else if (mCurrentAction == LOADING_ACTION.LOAD_MORE) {
            LoadingHelper.hideLoading(getSupportFragmentManager());
        }
        mCurrentAction = LOADING_ACTION.NONE;
    }

    private void processTransactionsList(GetTransactionsResponse getTransactionsResponse) {
        int itemCount = getTransactionsResponse.getTransactionSummaries().size();
        if (itemCount == 0)
            return;

        if (mCurrentAction == LOADING_ACTION.REFRESH) {
            transactionAdapter.clear();
        }

        for (int i = 0; i < itemCount; i++) {
            transactionAdapter.addToTransactionsList(getTransactionsResponse.getTransactionSummaries().get(i));
        }

        mCurrentAction = LOADING_ACTION.NONE;
        transactionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRefresh() {
        Logger.logInfo(TransactionsActivity.class, "onRefresh");
        mCurrentAction = LOADING_ACTION.REFRESH;
        action_load_more.setEnabled(false);
        fetchTransaction();
    }

    private void fetchTransaction() {
        try {

            GetTransactionsRequest getTransactionsRequest = new GetTransactionsRequest();
            getTransactionsRequest.setAuthToken(authToken);
            getTransactionsRequest.setLatest(true); // set to true to get the latest transactions
            getTransactionsRequest.setNumberOfRecords(20); // default is 10
            mSession = Mintegrate.getTransactions(getTransactionsRequest, new ResponseCallback<GetTransactionsResponse>() {
                @Override
                public void onCompletion(Session session, GetTransactionsResponse getTransactionsResponse) {
                    onCompletion(session, getTransactionsResponse);
                }

                @Override
                public void onError(Session session, MintegrateError.Error error) {
                    onError(session, error);
                }
            });
            mSession.next(); // start the session

        } catch (Exception e) {
        }
    }

    @Override
    public void onItemClick(GetTransactionsResponse.TransactionSummary transactionSummary) {
        Logger.logInfo(TransactionsActivity.class, "item on click");
        Intent intent = new Intent(this, TransactionDetailActivity.class);
        intent.putExtra("transaction", transactionSummary);
        intent.putExtra("authtoken", authToken);
        startActivity(intent);

    }

    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

        private final int mBottom;
        private final int mTop;
        private final int mLeft;
        private final int mRight;

        public SpaceItemDecoration(int top, int bottom, int left, int right) {
            mTop = top;
            mBottom = bottom;
            mLeft = left;
            mRight = right;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {

            int index = parent.getChildAdapterPosition(view);
            if (index == 0) {
                setOffsets(outRect, mTop, mBottom, mLeft, mRight);
            } else {
                setOffsets(outRect, 0, mBottom, mLeft, mRight);
            }
        }

        private void setOffsets(Rect outRect, int top, int bottom, int left, int right) {
            outRect.top = top;
            outRect.bottom = bottom;
            outRect.left = left;
            outRect.right = right;
        }
    }
}

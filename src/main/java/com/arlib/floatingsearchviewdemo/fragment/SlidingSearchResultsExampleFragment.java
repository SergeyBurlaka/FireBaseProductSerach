package com.arlib.floatingsearchviewdemo.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.arlib.floatingsearchview.util.Util;
import com.arlib.floatingsearchviewdemo.R;
import com.arlib.floatingsearchviewdemo.adapter.SearchResultsListAdapter;
import com.arlib.floatingsearchviewdemo.data.ColorWrapper;
import com.arlib.floatingsearchviewdemo.data.DataHelper;
import com.arlib.floatingsearchviewdemo.data.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SlidingSearchResultsExampleFragment extends BaseExampleFragment {
    private static final int FIRST_ITEM = 0 ;
    private final String TAG = "BlankFragment";

    public static final long FIND_SUGGESTION_SIMULATED_DELAY = 250;

    private FloatingSearchView mSearchView;

    private RecyclerView mSearchResultsList;
    private SearchResultsListAdapter mSearchResultsAdapter;

    private boolean mIsDarkSearchTheme = false;

    private String mLastQuery = "";

    private DatabaseReference firebaseDatabase;
    private EditText productText;
    private Button addProduct;

    private volatile List <Product> productsResultControlOptimization;
    private volatile ArrayList<Product> subProductResult;

    public SlidingSearchResultsExampleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sliding_search_results_example_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSearchView = (FloatingSearchView) view.findViewById(R.id.floating_search_view);
        mSearchResultsList = (RecyclerView) view.findViewById(R.id.search_results_list);
        productText = (EditText)view.findViewById(R.id.editText_put_product);

        //TODO set to model singleton!!
        productsResultControlOptimization = new ArrayList<>();

        addProduct = (Button)view.findViewById(R.id.button_add_product);
        addProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wrightNewPost (productText.getText().toString());

            }
        });

       firebaseDatabase = FirebaseDatabase.getInstance().getReference();

        setupFloatingSearch();
        setupResultsList();
        setupDrawer();
    }

    private void wrightNewPost(String newProductTitle) {
        List <String> subTitleList = new ArrayList<>();
        Map<String,Object> subTitles = new HashMap<>();

        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(newProductTitle);
        while (matcher.find()) {
            Log.d(TAG, "Add to FB: "+matcher.group());
            subTitleList.add(matcher.group());
            subTitles.put(matcher.group(), true);
        }

        //post all product names
        String keyProduct = firebaseDatabase.child("product-indexing").push().getKey();

        Map<String, Object> postValues =  new HashMap<>();
        Map<String, Object> childUpdates = new HashMap<>();

        postValues.put ( "title", newProductTitle );
        postValues.put("allSubTitles",subTitles);

        //childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/product-indexing/"+ keyProduct, postValues);
        firebaseDatabase.updateChildren(childUpdates);

        childUpdates = new HashMap<>();
        for (String dividTitle : subTitleList){

            String key = firebaseDatabase.child("product-namespace").push().getKey();
            postValues =  new HashMap<>();
            postValues.put ( "subTitle", dividTitle );
            postValues.put ("productKey", keyProduct);

            //childUpdates.put("/posts/" + key, postValues);
            childUpdates.put("/product-namespace/"+ key, postValues);

        }
        firebaseDatabase.updateChildren(childUpdates);
    }

    private void setupFloatingSearch() {
        mSearchView.setOnQueryChangeListener(new FloatingSearchView.OnQueryChangeListener() {

            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {

                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {


                  // List<ColorSuggestion> results = new ArrayList<>();
                    final List <String> filterResult = new ArrayList<>();

                    Pattern pattern = Pattern.compile("\\w+");
                    Matcher matcher = pattern.matcher(newQuery);
                    while (matcher.find()) {

                       // System.out.println(matcher.group());
                       // Log.d(TAG, matcher.group());
                        filterResult.add(matcher.group());
                      //  results.add(new ColorSuggestion(matcher.group()));
                        //Log.d(TAG, "search: "+results.toString());
                    }

                    Query queryRef = firebaseDatabase
                            .child("product-namespace")
                            .orderByChild("subTitle")
                            .startAt(filterResult.get(FIRST_ITEM))//;
                            .endAt(filterResult.get(FIRST_ITEM) + "\uf8ff");;

                    queryRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //Log.d(TAG, "From firebase: "+dataSnapshot.toString());

                            //productsResultControlOptimization = new ArrayList<>();

                            subProductResult = new ArrayList<Product>();
                            for (DataSnapshot task : dataSnapshot.getChildren()) {
                               //Log.d(TAG, "From firebase: "+task.getValue());


                                Map<String,Object> productData = (Map<String, Object>) task.getValue();
                               // productData.get("title");
                                //Log.d(TAG, "From firebase: "+ productData.get("productKey")+" subTitle: "+productData.get("subTitle"));

                                //Query in query

                                if ( checkIfProductExistOptimization((String) productData.get("productKey"))){

                                final String productKey = (String) productData.get("productKey");
                                Query queryRef = firebaseDatabase
                                        .child("product-indexing").child(productKey);
                                queryRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Map<String,Object> productData = (Map<String, Object>) dataSnapshot.getValue();
                                        Log.d(TAG, "From firebase: "+ productData.get("title"));
                                        Product p = new Product((String) productData.get("title"), productKey);
                                        productsResultControlOptimization.add(p);
                                        subProductResult.add(p);
                                        mSearchView.swapSuggestions(subProductResult);

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                                }else{

                                    mSearchView.swapSuggestions(subProductResult);

                                }


                            }

                        }

                        private synchronized boolean checkIfProductExistOptimization(String productKey) {
                           for (Product p: productsResultControlOptimization){
                               if(p.getProductKey().matches(productKey)) {
                                   subProductResult.add(p);
                                   return false;}
                           }
                            return true;
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG, "on cancell");
                        }


                    });

                    //this shows the top left circular progress
                    //you can call it where ever you want, but
                    //it makes sense to do it when loading something in
                    //the background.

                    //mSearchView.showProgress();

                    //simulates a query call to a data source
                    //with a new query.

                    /* DataHelper.findSuggestions(getActivity(), newQuery, 5,
                            FIND_SUGGESTION_SIMULATED_DELAY, new DataHelper.OnFindSuggestionsListener() {

                                @Override
                                public void onResults(List<ColorSuggestion> results) {

                                    //this will swap the data and
                                    //render the collapse/expand animations as necessary
                                    mSearchView.swapSuggestions(results);

                                    //let the users know that the background
                                    //process has completed
                                    mSearchView.hideProgress();
                                }
                            });*/

                  //  List<ColorSuggestion> results2;
                   // List<ColorSuggestion> results = new ArrayList<>();
                   // results.add(new ColorSuggestion("blue"));


                }

                Log.d(TAG, "onSearchTextChanged()");
            }

        });



        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(final SearchSuggestion searchSuggestion) {

                Product colorSuggestion = (Product) searchSuggestion;
                DataHelper.findColors(getActivity(), colorSuggestion.getBody(),
                        new DataHelper.OnFindColorsListener() {

                            @Override
                            public void onResults(List<ColorWrapper> results) {
                                mSearchResultsAdapter.swapData(results);
                            }

                        });
                Log.d(TAG, "onSuggestionClicked()");

                mLastQuery = searchSuggestion.getBody();
            }

            @Override
            public void onSearchAction(String query) {
                mLastQuery = query;

                DataHelper.findColors(getActivity(), query,
                        new DataHelper.OnFindColorsListener() {

                            @Override
                            public void onResults(List<ColorWrapper> results) {
                                mSearchResultsAdapter.swapData(results);
                            }

                        });
                Log.d(TAG, "onSearchAction()");
            }
        });

        mSearchView.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {

                //show suggestions when search bar gains focus (typically history suggestions)
                mSearchView.swapSuggestions(DataHelper.getHistory(getActivity(), 3));

                Log.d(TAG, "onFocus()");
            }

            @Override
            public void onFocusCleared() {

                //set the title of the bar so that when focus is returned a new query begins
                mSearchView.setSearchBarTitle(mLastQuery);

                //you can also set setSearchText(...) to make keep the query there when not focused and when focus returns
                //mSearchView.setSearchText(searchSuggestion.getBody());

                Log.d(TAG, "onFocusCleared()");
            }
        });


        //handle menu clicks the same way as you would
        //in a regular activity
        mSearchView.setOnMenuItemClickListener(new FloatingSearchView.OnMenuItemClickListener() {
            @Override
            public void onActionMenuItemSelected(MenuItem item) {

                if (item.getItemId() == R.id.action_change_colors) {

                    mIsDarkSearchTheme = true;

                    //demonstrate setting colors for items
                    mSearchView.setBackgroundColor(Color.parseColor("#787878"));
                    mSearchView.setViewTextColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setHintTextColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setActionMenuOverflowColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setMenuItemIconColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setLeftActionIconColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setClearBtnColor(Color.parseColor("#e9e9e9"));
                    mSearchView.setDividerColor(Color.parseColor("#BEBEBE"));
                    mSearchView.setLeftActionIconColor(Color.parseColor("#e9e9e9"));
                } else {

                    //just print action
                    Toast.makeText(getActivity().getApplicationContext(), item.getTitle(),
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        //use this listener to listen to menu clicks when app:floatingSearch_leftAction="showHome"
        mSearchView.setOnHomeActionClickListener(new FloatingSearchView.OnHomeActionClickListener() {
            @Override
            public void onHomeClicked() {

                Log.d(TAG, "onHomeClicked()");
            }
        });

        /*
         * Here you have access to the left icon and the text of a given suggestion
         * item after as it is bound to the suggestion list. You can utilize this
         * callback to change some properties of the left icon and the text. For example, you
         * can load the left icon images using your favorite image loading library, or change text color.
         *
         *
         * Important:
         * Keep in mind that the suggestion list is a RecyclerView, so views are reused for different
         * items in the list.
         */
        mSearchView.setOnBindSuggestionCallback(new SearchSuggestionsAdapter.OnBindSuggestionCallback() {
            @Override
            public void onBindSuggestion(View suggestionView, ImageView leftIcon,
                                         TextView textView, SearchSuggestion item, int itemPosition) {
                Product colorSuggestion = (Product) item;

                String textColor = mIsDarkSearchTheme ? "#ffffff" : "#000000";
                String textLight = mIsDarkSearchTheme ? "#bfbfbf" : "#787878";

                if (colorSuggestion.ismIsHistory()) {
                    leftIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.ic_history_black_24dp, null));

                    Util.setIconColor(leftIcon, Color.parseColor(textColor));
                    leftIcon.setAlpha(.36f);
                } else {
                    leftIcon.setAlpha(0.0f);
                    leftIcon.setImageDrawable(null);
                }

                textView.setTextColor(Color.parseColor(textColor));
                String text = colorSuggestion.getBody()
                        .replaceFirst(mSearchView.getQuery(),
                                "<font color=\"" + textLight + "\">" + mSearchView.getQuery() + "</font>");
                textView.setText(Html.fromHtml(text));
            }

        });

        //listen for when suggestion list expands/shrinks in order to move down/up the
        //search results list
        mSearchView.setOnSuggestionsListHeightChanged(new FloatingSearchView.OnSuggestionsListHeightChanged() {
            @Override
            public void onSuggestionsListHeightChanged(float newHeight) {
                mSearchResultsList.setTranslationY(newHeight);
            }
        });
    }

    private void setupResultsList() {
        mSearchResultsAdapter = new SearchResultsListAdapter();
        mSearchResultsList.setAdapter(mSearchResultsAdapter);
        mSearchResultsList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public boolean onActivityBackPress() {
        //if mSearchView.setSearchFocused(false) causes the focused search
        //to close, then we don't want to close the activity. if mSearchView.setSearchFocused(false)
        //returns false, we know that the search was already closed so the call didn't change the focus
        //state and it makes sense to call supper onBackPressed() and close the activity
        if (!mSearchView.setSearchFocused(false)) {
            return false;
        }
        return true;
    }

    public class FirebaseConstants {
        //productItem is branch item in firebase
        public static final String PRODUCT_SEARCH = "product-search";
        public static final String PRODUCT_ITEM = "product";
        public static final String PRODUCT_USER_LIKE = "product-user-like";
        public static final String PRODUCT_DETAIL ="product-detail" ;
    }

    private void setupDrawer() {
        attachSearchViewActivityDrawer(mSearchView);
    }

}

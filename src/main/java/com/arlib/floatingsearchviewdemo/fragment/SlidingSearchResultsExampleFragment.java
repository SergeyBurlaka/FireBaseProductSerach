package com.arlib.floatingsearchviewdemo.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
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
import com.arlib.floatingsearchviewdemo.data.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SlidingSearchResultsExampleFragment extends BaseExampleFragment {
    private static final int FIRST_KEYWORD = 0 ;
    private static final int SECOND_KEYWORD_INPUTED = 2;
    private final String TAG = "BlankFragment";
    public static final long FIND_SUGGESTION_SIMULATED_DELAY = 250;
    private FloatingSearchView mSearchView;

    private RecyclerView mSearchResultsList;


    private boolean mIsDarkSearchTheme = false;

    private String mLastQuery = "";

    private DatabaseReference firebaseDatabase;
    private EditText productText;
    private Button addProduct;


    //TODO_ set to model singleton!!
    private volatile Set<Product> productsResultCashOptimization;



    private Stack<List<Product>> backStackDeltaLastProductResult = new Stack<>();
    private int FIRST_WORDS_LIMIT = 4;

    public SlidingSearchResultsExampleFragment() {/*Required empty public constructor*/}

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


        productsResultCashOptimization = new HashSet<>();


        addProduct = (Button)view.findViewById(R.id.button_add_product);
        addProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wrightNewPost (productText.getText().toString());
            }
        });

       firebaseDatabase = FirebaseDatabase.getInstance().getReference();

        setupFloatingSearch();
        //todo_ setup result list
        setupResultsList();
        setupDrawer();
    }

    //todo wrightNewPost(String newProductTitle)
    private void wrightNewPost(String newProductTitle) {
        List <String> subTitleList = new ArrayList<>();
        Map<String,Object> subTitles = new HashMap<>();

        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(newProductTitle);

        //limit for saving data in namespace/optimization feature
        int iteratorWordsLimit = FIRST_WORDS_LIMIT;
        while (matcher.find()) {
           // Log.d(TAG, "Add to FB: "+matcher.group());
            if (iteratorWordsLimit> 0)subTitleList.add(matcher.group().toLowerCase());
            subTitles.put(matcher.group().toLowerCase(), true);
            iteratorWordsLimit--;
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

           // String key = firebaseDatabase.child("product-namespace").push().getKey();
            postValues =  new HashMap<>();

            //postValues.put ( "subTitle", dividTitle );
            //postValues.put ("productKey", keyProduct);

            //childUpdates.put("/posts/" + key, postValues);

            childUpdates.put("/product-namespace/"+ dividTitle+"/subTitle", dividTitle);
            childUpdates.put("/product-namespace/"+ dividTitle+"/productKey/"+keyProduct, true);
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

                    final List <String> userKeyWordInput = new ArrayList<>();

                    Pattern pattern = Pattern.compile("\\w+");
                    Matcher matcher = pattern.matcher(newQuery);
                    while (matcher.find()) {
                        userKeyWordInput.add(matcher.group().toLowerCase());
                    }

                    if (userKeyWordInput.isEmpty()|| userKeyWordInput.get(FIRST_KEYWORD).length() <2)return;

                    Iterator iteratorKeyWord = userKeyWordInput.iterator();
                    List <String> productIDList = new ArrayList<>();

                    queryListProductId (iteratorKeyWord, productIDList, true);

                }
                Log.d(TAG, "onSearchTextChanged()");
            }

            //todo qreate Object for parmeters like new SearchProductItem ();
            private void queryListProductId(final Iterator iteratorKeyWord, List<String> productIDList1,  boolean firstIteration) {
                final boolean isFirst = firstIteration;
                final List <String> productIDList = productIDList1;
                final List <String> productIntersectionIDList = new ArrayList<>();

                final String keyItem = (String) iteratorKeyWord.next();

                getQuery(keyItem).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot task : dataSnapshot.getChildren()) {
                           List <String> tempProductIDList = parseFirebaseData(task,keyItem );

                            if (productIDList.isEmpty()&& isFirst){
                                for (String key: tempProductIDList ) {
                                    productIntersectionIDList.add(key);
                                }
                            }else{
                                for (String key: productIDList) {
                                    if(tempProductIDList.contains(key)) {
                                        productIntersectionIDList.add(key);
                                    }
                                }
                            }
                        }

                        if(iteratorKeyWord.hasNext()){
                            queryListProductId(iteratorKeyWord,  productIntersectionIDList, false);
                        }else{
                            makeQueryProduct( productIntersectionIDList);
                        }
                    }

                    private void makeQueryProduct(List<String> productIntersectionIDList) {
                       final List<Product> showingProductResultList = new ArrayList<>();
                        if (productIntersectionIDList.isEmpty()){
                            mSearchView.clearSuggestions();
                            return;
                        }

                        for (final String productKey : productIntersectionIDList){

                        Query queryRef = firebaseDatabase
                                .child("product-indexing").child(productKey).orderByChild(productKey);
                        queryRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Map<String,Object> productData = (Map<String, Object>) dataSnapshot.getValue();
                                Product p = new Product(
                                        (String) productData.get("title"),
                                        productKey,
                                        (Map<String, Object>) productData.get("allSubTitles")
                                );

                                showingProductResultList.add(p);
                                mSearchView.swapSuggestions(showingProductResultList);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "on cancell");
                    }
                });
            }

            //todo  Query queryRef = firebaseDatabase
            public Query getQuery(String keyItem) {
                return   firebaseDatabase
                        .child("product-namespace")
                        .orderByChild("subTitle")
                        .startAt(keyItem)
                        .endAt( keyItem + "\uf8ff")
                        .limitToFirst(50);
            }

            //todo return ProductSearchObject
            public List <String> parseFirebaseData(DataSnapshot task, String userKeyWord) {

                Map<String,Object> productData = (Map<String, Object>) task.getValue();
                List<String> productKeyList = new ArrayList<>();

                String keyName = (String)productData.get("subTitle");
                if(!isNameValid(userKeyWord,keyName)) return productKeyList;

               Map <String,Object>productKeyMap = (Map<String, Object>) productData.get("productKey");
                for (String keyProduct: productKeyMap.keySet() ){
                    productKeyList.add(keyProduct);
                }
                return productKeyList;
            }

            public boolean isNameValid (String userKeyName, String productKeyName){
                    if(Pattern.compile(Pattern.quote(userKeyName), Pattern.CASE_INSENSITIVE).matcher(productKeyName).find()){
                        return true;}

                return false;
            }
        });



        mSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(final SearchSuggestion searchSuggestion) {

                Product colorSuggestion = (Product) searchSuggestion;

                //todo_ onSuggestionClicked
                Log.d(TAG, "onSuggestionClicked()");

                mLastQuery = searchSuggestion.getBody();
            }

            @Override
            public void onSearchAction(String query) {
               //todo_ from here show search products in List_
                mLastQuery = query;
                Log.d(TAG, "onSearchAction()");
            }
        });

        mSearchView.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {
                //show suggestions when search bar gains focus (typically history suggestions)
               // mSearchView.swapSuggestions(DataHelper.getHistory(getActivity(), 3));
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
       // mSearchResultsAdapter = new SearchResultsListAdapter();
       // mSearchResultsList.setAdapter(mSearchResultsAdapter);
       // mSearchResultsList.setLayoutManager(new LinearLayoutManager(getContext()));
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

    private void setupDrawer() {
        attachSearchViewActivityDrawer(mSearchView);
    }

}

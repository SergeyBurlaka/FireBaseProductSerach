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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SlidingSearchResultsExampleFragment extends BaseExampleFragment {
    private static final int FIRST_KEYWORD = 0 ;
    private static final int NEXT_KEYWORD_SIZE = 2;
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

    //TODO_ set to model singleton!!
    private volatile List <Product> productsResultCashOptimization;
    private volatile List<Product> showingProductResultList;
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


        productsResultCashOptimization = new ArrayList<>();


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

    //todo   1)wrightNewPost(String newProductTitle)
    private void wrightNewPost(String newProductTitle) {
        List <String> subTitleList = new ArrayList<>();
        Map<String,Object> subTitles = new HashMap<>();

        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher = pattern.matcher(newProductTitle);

        //limit for saving data in namespace/optimization feature
        int iteratorWordsLimit = FIRST_WORDS_LIMIT;
        while (matcher.find()) {
            Log.d(TAG, "Add to FB: "+matcher.group());
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
            /*
            * In cause user next key word input in search view
           */
            public List<? extends SearchSuggestion> getShowProductListFromCash(
                    String oldQuery, String newQuery, List <String>userKeyWordInput
            ) {
                // Check old and new query
                // returning to last state if old query > new query
                if (oldQuery.length()>newQuery.length()) {

                    //BACK STACK FEATURE - get products from back stack
                    // In cause of back one character return
                    // we going also back in stack of products from last filter query using delta product

                    List<Product> productsFromBSback = backStackDeltaLastProductResult.pop();
                    for (Product product: productsFromBSback){
                         showingProductResultList.add(product );
                    }
                    return showingProductResultList;

                } else
                {
                    return   showingProductResultList = goSearchFilterFromCash(userKeyWordInput);
                }
            }

            @Override
            public void onSearchTextChanged(String oldQuery, final String newQuery) {

                if (!oldQuery.equals("") && newQuery.equals("")) {
                    mSearchView.clearSuggestions();
                } else {
                    final List <String> userKeyWordInput = new ArrayList<>();


                    //split user phrase into key words
                    Pattern pattern = Pattern.compile("\\w+");
                    Matcher matcher = pattern.matcher(newQuery);

                    //and put keyword to user keyword list
                    while (matcher.find()) {userKeyWordInput.add(matcher.group().toLowerCase());}

                    //also add dummy in cause of " "
                    if (newQuery.substring(newQuery.length() - 1)==" ") backStackDeltaLastProductResult.push(new ArrayList<Product>());
                    /*
                    * In cause user next key word input in search view
                    */
                     if(userKeyWordInput.size() >= NEXT_KEYWORD_SIZE){
                        mSearchView.swapSuggestions(getShowProductListFromCash( oldQuery, newQuery, userKeyWordInput)); return;
                     }

                    backStackDeltaLastProductResult = new Stack<>();

                    /*
                    *  Search with user first key word
                    */
                    //query to get product id if user key word exist in firebase
                    //todo ->12:35 set limit in query
                    //todo 2)  Query queryRef = firebaseDatabase
                    Query queryRef = firebaseDatabase
                            .child("product-namespace")
                            .orderByChild("subTitle")
                            .startAt(userKeyWordInput.get(FIRST_KEYWORD))
                            .endAt(userKeyWordInput.get(FIRST_KEYWORD) + "\uf8ff");

                    queryRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            //put Product objects for showing in result menu
                            showingProductResultList = new ArrayList<>();

                            Log.d(TAG, "From firebase: "+ dataSnapshot.toString());

                            for (DataSnapshot task : dataSnapshot.getChildren()) {


                              Map<String,Object> productData = (Map<String, Object>) task.getValue();

                                //Some optimization of product queries amount via cashing getting Product in memory
                              List<String> productKeyMap =   checkIfProductExistOptimization((Map<String, Object>) productData.get("productKey"));

                                Log.d(TAG, "productData: "+ productKeyMap.toString() );

                                if(productKeyMap.isEmpty()){mSearchView.swapSuggestions(showingProductResultList); return;}
                                 for (final String productKey: productKeyMap){
                                    //getting product key as result
                                   // final Map<String, Object> productKey = (Map<String, Object>) productData.get("productKey");

                                    //trying to get product object with product key
                                    Query queryRef = firebaseDatabase
                                            .child("product-indexing").child(productKey);
                                    queryRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Map<String,Object> productData = (Map<String, Object>) dataSnapshot.getValue();

                                            Log.d(TAG, "From firebase: "+ productData.get("title"));

                                            Product p = new Product(
                                                    (String) productData.get("title"),
                                                    productKey,
                                                    (Map<String, Object>) productData.get("allSubTitles")
                                            );

                                            //cashing Product object result in base
                                            productsResultCashOptimization.add(p);

                                            //add next product to current queries list for showing in result
                                            //todo 13:44--> synchronized check if such product exist
                                            showingProductResultList.add(p);
                                            //swap list in result view
                                            mSearchView.swapSuggestions(showingProductResultList);

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }

                                    /*}else{
                                        mSearchView.swapSuggestions(showingProductResultList);
                                    }*/
                                }
                            }

                        //synchronized because firebase operations are multithreading
                        private synchronized List<String>  checkIfProductExistOptimization(Map<String, Object> productKeyMap) {
                            List <String> returnProductKeyMap = new ArrayList<>();

                            /*for( String keyName : productKeyMap.){

                            }*/
                            for (String key: productKeyMap.keySet() ) {
                                returnProductKeyMap.add(key);
                            for (Product p: productsResultCashOptimization){
                                   if (p.getProductKey().matches(key)) {
                                       Log.d(TAG, "*+%% @Product in cash "+ p.getProductName());
                                       showingProductResultList.add(p);
                                       returnProductKeyMap.remove(key);
                                   }

                               }
                           }
                            Log.d(TAG, "```% @List result /afterfilter "+ returnProductKeyMap.toString());
                            return returnProductKeyMap;
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG, "on cancell");
                        }
                    });
                }
                Log.d(TAG, "onSearchTextChanged()");
            }

            private List<Product> goSearchFilterFromCash(List<String> filterResult)
            {
                List <Product> newShowProductList = new ArrayList<>();
                List <Product>  productToBackStack = new ArrayList<>();
                String userKeyWordStr = filterResult.get(filterResult.size()-1 );

                for (Product product: showingProductResultList ){
                    if(product.isNameValid(userKeyWordStr)) newShowProductList.add(product);
                    // or making BACK STACK FEATURE:
                   //mean, if invalid product - just save it in delta for stack return
                    else productToBackStack.add(product);

                }
                // making BACK STACK FEATURE:
                // Save last state result commit in stack
                // with delta (equals to invalid product in new query result)
                backStackDeltaLastProductResult.push(productToBackStack);
                return newShowProductList;
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

    private void setupDrawer() {
        attachSearchViewActivityDrawer(mSearchView);
    }

}

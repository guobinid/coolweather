package com.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.db.City;
import com.android.db.County;
import com.android.db.Province;
import com.android.util.HttpUtil;
import com.android.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressBar progressBar;

    private TextView titleText;
    private Button backButton;
    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;

    //选中的省
    private Province selectProvince;

    //选中的市
    private City selectCity;

    //当前选中的级别
    private int currenLevel;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        progressBar = view.findViewById(R.id.progessBar);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currenLevel == LEVEL_PROVINCE){
                    selectProvince = provinceList.get(position);
                    //查询市级的数据
                    queryCitites();
                }else if(currenLevel == LEVEL_CITY){
                    selectCity = cityList.get(position);
                    //查询所有县级数据
                    queryCounties();
                }else if(currenLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        Log.d("mmm","weatherId--->" + weatherId);
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("mmm","currenLevel" + currenLevel);
                if(currenLevel == LEVEL_COUNTY){
                    //返回上一级，查询市级所有数据
                    queryCitites();
                }else if(currenLevel == LEVEL_CITY){
                    //返回上一级,查询所有省级数据
                    queryProvince();
                }
                //都不是，默认查询省级数据
            }
        });
        queryProvince();
    }
    //查询所有的省级数据，如果查不到就从网络上获取
    private void queryProvince(){
        titleText.setText("china");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            //从网络获取数据
            queryFromServer(address,"province");
        }
    }

    //查询所有市级数据，如果查询不到就从网上获取
    private void queryCitites(){
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
    //查询所有县级数据，如果查询不到就从网上获取
    private void queryCounties(){
        titleText.setText(selectCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?",String.valueOf(selectCity.getId())).find(County.class);
        if(countyList.size() > 0){
            dataList.clear();
            for(County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            int cityCode = selectCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }

    //根据地址和类型从服务器上去查找数据
    private void queryFromServer(String address,final String type){
        showProgressBar();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressBar();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                Log.d("mmm","responseText-->"+responseText) ;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressBar();
                            if("province".equals(type)){
                                queryProvince();
                            }else if("city".equals(type)){
                                queryCitites();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressBar(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void closeProgressBar(){
        progressBar.setVisibility(View.GONE);
    }

}

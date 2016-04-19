package com.mufengweather.app.activity;

import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
import android.app.PendingIntent.OnFinished;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mufengweather.app.R;
import com.mufengweather.app.db.MufengWeatherDB;
import com.mufengweather.app.model.City;
import com.mufengweather.app.model.County;
import com.mufengweather.app.model.Province;
import com.mufengweather.app.util.HttpCallbackListener;
import com.mufengweather.app.util.HttpUtil;
import com.mufengweather.app.util.Utility;

public class ChooseAreaActivity extends Activity {
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private MufengWeatherDB mufengWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中的省份
	 */
	private Province selectedProvince;
	/**
	 * 选中的城市
	 */
	private City selectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView)findViewById(R.id.list_view);
		titleText = (TextView)findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this,android.R.layout.simple_expandable_list_item_1,dataList);
		listView.setAdapter(adapter);
		mufengWeatherDB = MufengWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			if(currentLevel == LEVEL_PROVINCE){
				selectedProvince = provinceList.get(position);
				queryCities();
			}else if(currentLevel == LEVEL_CITY){
				selectedCity = cityList.get(position);
				queryCounties();
			}
		}
		});
		queryProvinces(); // 加载省级数据 
	}
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces(){
		provinceList = mufengWeatherDB.loadProvinces();
		if(provinceList.size() > 0){
			dataList.clear();
			for(Province province: provinceList){
				dataList.add(province.getProvinceName());
				adapter.notifyDataSetChanged();
				listView.setSelection(0);//将列表移动到指定的Position处。
				titleText.setText("中国");
				currentLevel = LEVEL_PROVINCE;
			}
		}else{
			queryFromServer(null,"province");
		}
		
		
	}
	/**
	 * 查询全国所有的城市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		cityList = mufengWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0){
			dataList.clear();
			for(City city : cityList){
				dataList.add(city.getCityName());
				adapter.notifyDataSetChanged();
				listView.setSelection(0);
				titleText.setText(selectedProvince.getProvinceName());
				currentLevel = LEVEL_CITY;
			}
		}else{
			queryFromServer(selectedProvince.getProvinceCode(),"city");
			Log.d("data",selectedProvince.getProvinceCode() );
		}
	}
	/**
	 * 查询全国所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		countyList = mufengWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0){
			dataList.clear();
			for(County county : countyList){
				dataList.add(county.getCountyName());
				adapter.notifyDataSetChanged();
				listView.setSelection(0);
				titleText.setText(selectedCity.getCityName());
				currentLevel = LEVEL_COUNTY;
			}
		}else{
			String str1 = selectedCity.getCityCode();
			String str = selectedProvince.getProvinceCode()+selectedCity.getCityCode();
			queryFromServer(selectedCity.getCityCode(),"county");
			Log.d("str", str1);
			
		}
	}
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据。
	 */
	private void queryFromServer(final String code, final String type){
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();               
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){

			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvincesResponse(mufengWeatherDB, response);
				}else if("city".equals(type)){
					result = Utility.handleCitiesResponse(mufengWeatherDB, response, selectedProvince.getId());
				}else if("county".equals(type)){
					result = Utility.handleCountiesResponse(mufengWeatherDB, response, selectedCity.getId());
				}
				if(result){
					//通过runOnUiThread()方法回到主线程处理逻辑。
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}else if("city".equals(type)){
								queryCities();
							}else if("county".equals(type)){
								queryCounties();
							}
						}
						
					});
				}
			}

			@Override
			public void onError(Exception e) {

				//通过runOnUiThread()方法回到主线程处理逻辑。
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						//closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
					}
				});
			
				
			}
			

		});
	}
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog(){
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog(){
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}
	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表，省列表，还是直接退出。
	 */
	 @Override
	public void onBackPressed() {
		 if(currentLevel == LEVEL_COUNTY){
			 queryCities();
		 }else if(currentLevel == LEVEL_CITY){
			 queryProvinces();
		 }else{
			 finish();
		 }
	}
}

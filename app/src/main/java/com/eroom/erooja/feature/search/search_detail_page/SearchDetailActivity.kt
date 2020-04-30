package com.eroom.erooja.feature.search.search_detail_page


import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.eroom.data.entity.GoalContent
import com.eroom.data.localclass.DesignClass
import com.eroom.data.localclass.DevelopClass
import com.eroom.data.response.InterestedGoalsResponse
import com.eroom.domain.utils.getKeyFromValue
import com.eroom.domain.utils.toastLong
import com.eroom.erooja.R
import com.eroom.erooja.databinding.ActivitySearchDetailBinding
import com.eroom.erooja.feature.search.search_detail_frame.*
import com.eroom.erooja.singleton.JobClassHashMap
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding.widget.RxTextView
import kotlinx.android.synthetic.main.activity_search_detail.*
import org.koin.android.ext.android.get
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 */
class SearchDetailActivity : AppCompatActivity(), SearchDetailContract.View {
    private var changeNum: Int = 0
    private lateinit var searchDetailFrame: ArrayList<Fragment>
    private val searchword: MutableLiveData<String> = MutableLiveData()
    private lateinit var searchAdapter: ArrayAdapter<String>
    private var autosearch = ArrayList<String>()

    private lateinit var searchDetailBinding: ActivitySearchDetailBinding
    private lateinit var presenter: SearchDetailPresenter

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpDataBinding()
        initView()
        initFragment()
    }

    private fun initFragment() {
        searchDetailFrame = ArrayList()
        searchDetailFrame.apply {
            addAll(
                listOf(
                    SearchJobClassFragment.newInstance(),
                    SearchJobGoalFragment.newInstance(),
                    SearchNoContentFragment.newInstance(),
                    SearchResultFragment.newInstance()
                )
            )
        }.map {
            it.apply {
                supportFragmentManager.beginTransaction()
                    .add(R.id.search_container, it).commit()
            }
        }.run {
            loadFragment(0)
        }

        presenter = SearchDetailPresenter(this, get())
    }

    private fun loadFragment(index: Int) =
        searchDetailFrame.map {
            it.apply { supportFragmentManager.beginTransaction().hide(this).commit() }
        }[index].also {
            supportFragmentManager.beginTransaction().show(it).commit()
        }

    private fun setUpDataBinding(){
        searchDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_detail)
        searchDetailBinding.searchdetail = this@SearchDetailActivity
    }

    private fun initView() {
        searchDetailBinding.searchTablayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabSelected(p0: TabLayout.Tab) {
                changeNum = p0.position
                changeView(changeNum)
            }
        })

        for (i in 0 until 9) {
            autosearch.add(DesignClass.getArray()[i].getName())
            autosearch.add(DevelopClass.getArray()[i].getName())
        }

        searchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, autosearch)
        searchDetailBinding.searchEditText.setAdapter(searchAdapter)

        RxTextView.textChanges(searchDetailBinding.searchEditText)
            .debounce(500, TimeUnit.MILLISECONDS)
            .map { it.toString() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                //Todo
                searchword.value = it
                //Timber.i("${searchword.value}")

                if(searchword.value == ""){
                    loadFragment(changeNum)
                }

                when (changeNum) {
                    0 -> { //직무
                        val key =
                            JobClassHashMap.hashmap.getKeyFromValue(searchword.value.toString())
                        presenter.getSearchJobInterest(key)
                        key?.let {
                            (searchDetailFrame[3] as SearchResultFragment).apply{
                                arguments = Bundle().apply {
                                    putLong("key", key)
                                }
                                (searchDetailFrame[3] as SearchResultFragment).setKey()
                            }
                        }?: run{
                            loadFragment(2)
                        }
                    }

                    1 -> {//목표
                       val title =
                           URLEncoder.encode(searchword.value, "UTF-8")
                        presenter.getSearchGoalTitle(title)
                        (searchDetailFrame[3] as SearchResultFragment).setTitle(title)
                    }
                }
            }
    }

    override fun changeView(pos: Int){
        when(pos){
            0 -> loadFragment(0)
            1 -> loadFragment(1)
        }
    }

    override fun checkContentSize(size: Int) {
        if(size == 0){
            loadFragment(2)
        }
        else {
            loadFragment(3)
        }
    }

    fun back(v: View){
        finish()
    }
}



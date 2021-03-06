package com.example.chownow.ui.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chownow.MainApplication
import com.example.chownow.R
import com.example.chownow.data.model.Locations
import com.example.chownow.data.model.RestaurantLocation
import com.example.chownow.databinding.FragmentListLocationsBinding
import com.example.chownow.utils.viewModelProvider
import javax.inject.Inject
import kotlin.properties.Delegates


class LocationsListFragment: Fragment() {

    private val DEFAULTRESTAURANT: String = "1"

    private lateinit var locationsListAdapter: LocationsListAdapter
    private val appComponents by lazy { MainApplication.appComponents }
    private lateinit var restaurantInformation: Locations

    private var listener: OnLocationSelectedListener? = null
    private var isDualPane by Delegates.notNull<Boolean>()


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private fun getViewModel(): LocationsListFragmentViewModel {
        return viewModelProvider(viewModelFactory)
    }

    private lateinit var binding: FragmentListLocationsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        appComponents.inject(this)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list_locations, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDualPane = resources.getBoolean(R.bool.twoPaneMode);

        initViews()
        initObservers()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnLocationSelectedListener) {
            context as OnLocationSelectedListener
        } else {
            throw ClassCastException("$context must implemenet OnLocationSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun initViews() {
        getViewModel().getLocationsForRestaurant(DEFAULTRESTAURANT)
    }

    private fun initObservers() {
        getViewModel().resultLocations.observe(viewLifecycleOwner, Observer { locationsList ->
            locationsList?.let {
                restaurantInformation = it
                binding.restaurantName.text = it.name
                initRecycler(it.locations)
                }
            }
        )

        getViewModel().errorMessage.observe(viewLifecycleOwner, Observer {
            Toast.makeText(this.context,"Connection Error", Toast.LENGTH_LONG).show();
        })

        getViewModel().selectItem.observe(viewLifecycleOwner, Observer {
            listener?.onLocationSelected(restaurantInformation.id, restaurantInformation.locations[it].id)
        })
    }

    private fun initRecycler(list: List<RestaurantLocation>) {
        if (!list.isNullOrEmpty()) {
            locationsListAdapter = LocationsListAdapter(list, getViewModel())
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = locationsListAdapter
            }
        }
    }
}

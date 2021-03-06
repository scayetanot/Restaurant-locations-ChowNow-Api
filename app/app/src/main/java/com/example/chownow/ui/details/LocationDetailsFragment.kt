package com.example.chownow.ui.details

import android.content.Intent
import android.net.Uri
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
import com.example.chownow.MainApplication.Companion.appComponents
import com.example.chownow.R
import com.example.chownow.data.model.DineInItem
import com.example.chownow.data.model.OpeningHours
import com.example.chownow.databinding.FragmentLocationDetailsBinding
import com.example.chownow.ui.main.MainActivity.Companion.LOCATION_ID
import com.example.chownow.ui.main.MainActivity.Companion.RESTAURANT_ID
import com.example.chownow.utils.formatPhoneNumber
import com.example.chownow.utils.remainingTime
import com.example.chownow.utils.viewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import javax.inject.Inject


class LocationDetailsFragment: Fragment() {

    private lateinit var binding: FragmentLocationDetailsBinding
    private var restaurantId: String? = ""
    private var locationId: String? = ""
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mapGoogle: GoogleMap

    private lateinit var openingHoursListAdapter: OpeningHoursListAdapter

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private fun getViewModel(): LocationDetailsFragmentViewModel {
        return viewModelProvider(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        appComponents.inject(this)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_location_details, container,false)
        restaurantId = arguments?.getString(RESTAURANT_ID)
        locationId = arguments?.getString(LOCATION_ID)

        binding.backButton.setOnClickListener { activity?.onBackPressed(); }

        mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        if (mapFragment != null) {
            try{
                mapFragment.getMapAsync(OnMapReadyCallback {
                    mapGoogle = it
                })
            } catch (e: Exception) {
                Toast.makeText(this.context, "Error - Impossible to load Map", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this.context, "Error - Impossible to load Map", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
    }

    private fun initViews(){
        getViewModel().getLocationDetails(restaurantId, locationId)
    }

    private fun initObservers(){
        getViewModel().resultLocations.observe(viewLifecycleOwner, Observer { locationsList ->
            locationsList?.let {
                if(it.fulfillment.dineIn != null) addDineInInformation(binding, it.fulfillment.dineIn)
                if(!it.cuisines.isNullOrEmpty()) addTypeOfCuisines(binding, it.cuisines)

                if((it.address.latitude != null) && (it.address.longitude != null)) {
                    locationLocationOnMap(it.address.latitude, it.address.longitude, it.name)
                } else {
                    Toast.makeText(this.context,"GPS Location not available", Toast.LENGTH_LONG).show();
                }

                binding.name.text = it.name
                binding.address.text = it.address.formattedAddress.replace(",","\n")

                if(!it.phone.isNullOrEmpty()) addPhoneNumberAndClickableAction(binding, it.phone)
            }
        }
        )

        getViewModel().errorMessage.observe(viewLifecycleOwner, Observer {
            Toast.makeText(this.context,"Connection Error", Toast.LENGTH_LONG).show();
        })
    }

    private fun addDineInInformation(binding: FragmentLocationDetailsBinding, dineIn: DineInItem){
        binding.openOrClose.text = if(dineIn.isAvailableNow) {
            getString(R.string.currently_open)
        } else {
            getString(R.string.currently_close)
        }

        if((dineIn.isAvailableNow != null)
            && dineIn.isAvailableNow
            && !dineIn.nextClosedTime.isNullOrEmpty()) {
            binding.remainingTime.text = remainingTime(dineIn.nextClosedTime)
            binding.remainingTime.visibility = View.VISIBLE
        } else {
            binding.remainingTime.visibility = View.GONE
        }

        if(!dineIn.displayHours.isNullOrEmpty()){
            initListView(dineIn.displayHours)
        }
    }

    private fun addTypeOfCuisines(binding: FragmentLocationDetailsBinding, typeOfCuisines: List<String>) {
        binding.typeOfCuisine.text = typeOfCuisines.joinToString(", ")
    }

    private fun addPhoneNumberAndClickableAction(binding: FragmentLocationDetailsBinding, phone: String?) {
        binding.phone.text = formatPhoneNumber(phone)
        binding.phone.setOnClickListener {
            try {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$phone")
                startActivity(callIntent)
            } catch (e: Exception) {
                Toast.makeText(this.context,"Sorry we cannot dial for you", Toast.LENGTH_LONG).show();
            }
        }
    }

    private fun locationLocationOnMap(lat: Double, lon: Double, name: String?) {

        val place = LatLng(lat, lon)
        val cameraLocation: CameraPosition = CameraPosition.Builder().
                                target(place)
                                .zoom(15.5f)
                                .bearing(0f)
                               // .tilt(25f)
                                .build()

        mapGoogle.addMarker(MarkerOptions().position(place).title(name))
        mapGoogle.moveCamera(CameraUpdateFactory.newCameraPosition(cameraLocation))
    }

    private fun initListView(openingList: List<OpeningHours>){
        openingHoursListAdapter = OpeningHoursListAdapter(openingList)
        binding.openingHours.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = openingHoursListAdapter
        }

    }
}
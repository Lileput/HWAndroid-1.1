package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.util.MapKitInit
import ru.netology.nmedia.databinding.FragmentPostMapBinding
import ru.netology.nmedia.dto.Coordinates
import ru.netology.nmedia.util.MapHelper
import ru.netology.nmedia.util.YandexMapLifecycle
import ru.netology.nmedia.util.registerMapLocationPermissionRequest

class PostMapFragment : Fragment() {

    private var _binding: FragmentPostMapBinding? = null
    private val binding get() = _binding!!

    private var pickedCoords: Coordinates? = null
    private var mapConfigured = false
    private lateinit var requestMapLocationPermission: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMapLocationPermission = registerMapLocationPermissionRequest()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPostMapBinding.inflate(inflater, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.pick_location)
            setDisplayHomeAsUpEnabled(true)
        }

        if (!MapKitInit.isApiKeyConfigured()) {
            Toast.makeText(requireContext(), R.string.mapkit_api_key_missing, Toast.LENGTH_LONG).show()
        }

        pickedCoords = readInitialCoords() ?: MapHelper.defaultLocation
        updateCoordsText(pickedCoords!!)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_post_map, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.confirm -> confirmSelection()
                        else -> false
                    }
            },
            viewLifecycleOwner,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestMapLocationPermission()
    }

    override fun onStart() {
        super.onStart()
        if (!MapKitInit.isApiKeyConfigured()) return
        YandexMapLifecycle.startMap(binding.mapView)
        configureMapIfNeeded()
    }

    override fun onStop() {
        if (MapKitInit.isApiKeyConfigured()) {
            YandexMapLifecycle.stopMap(binding.mapView)
        }
        super.onStop()
    }

    private fun configureMapIfNeeded() {
        if (mapConfigured) return
        mapConfigured = true

        val initial = pickedCoords ?: MapHelper.defaultLocation
        MapHelper.setupPickerMap(
            context = requireContext(),
            mapView = binding.mapView,
            initial = initial,
            onCoordsChanged = { coords ->
                pickedCoords = coords
                updateCoordsText(coords)
            },
        )
    }

    private fun confirmSelection(): Boolean {
        val coords = pickedCoords
        if (coords == null) {
            Toast.makeText(requireContext(), R.string.map_pick_required, Toast.LENGTH_SHORT).show()
            return false
        }
        setFragmentResult(
            REQUEST_PICK_COORDS,
            bundleOf(ARG_LAT to coords.lat, ARG_LNG to coords.long),
        )
        findNavController().navigateUp()
        return true
    }

    private fun readInitialCoords(): Coordinates? {
        val args = arguments ?: return null
        if (!args.containsKey(ARG_LAT) || !args.containsKey(ARG_LNG)) return null
        return Coordinates(lat = args.getDouble(ARG_LAT), long = args.getDouble(ARG_LNG))
    }

    private fun updateCoordsText(coords: Coordinates) {
        binding.coordsText.text = getString(
            R.string.location_selected,
            coords.lat,
            coords.long,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapConfigured = false
        _binding = null
    }

    companion object {
        const val REQUEST_PICK_COORDS = "pick_coords_result"
        const val ARG_LAT = "lat"
        const val ARG_LNG = "lng"

        fun createArguments(coords: Coordinates?): Bundle =
            if (coords == null) {
                bundleOf()
            } else {
                bundleOf(ARG_LAT to coords.lat, ARG_LNG to coords.long)
            }
    }
}

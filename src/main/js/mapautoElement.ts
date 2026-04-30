await google.maps.importLibrary("places");

function addMapAutoCompleteElement(input, callback) {
  const placeAutocomplete = new google.maps.places.PlaceAutocompleteElement();
  input.addendChild(placeAutocomplete);

  placeAutocomplete.addEventListener("gmp-placeselect", async ({ placePrediction }) => {
    const place = placePrediction.toPlace(); // Convert prediction to a Place object
    await place.fetchFields({ fields: ["displayName", "formattedAddress", "location"] }); // Fetch desired place details

    // Now you can use the 'place' object to access details like:
    console.log(place.displayName);
    console.log(place.formattedAddress);
    console.log(place.location.lat(), place.location.lng());
    callback(place);
  });
}
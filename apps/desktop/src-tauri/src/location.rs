/// Photon / OSM place search with a real User-Agent (webview fetch often gets 403).
#[tauri::command]
pub fn suggest_locations(query: String) -> Result<String, String> {
    let q = query.trim();
    if q.chars().count() < 2 {
        return Ok("{\"features\":[]}".into());
    }
    let body = ureq::get("https://photon.komoot.io/api/")
        .set(
            "User-Agent",
            "ContinuumCalendar/0.17.3 (location-suggest; +https://github.com/edwardlthompson/continuum-calendar)",
        )
        .set("Accept", "application/json")
        .query("q", q)
        .query("limit", "8")
        .timeout(std::time::Duration::from_secs(4))
        .call()
        .map_err(|e| e.to_string())?
        .into_string()
        .map_err(|e| e.to_string())?;
    Ok(body)
}

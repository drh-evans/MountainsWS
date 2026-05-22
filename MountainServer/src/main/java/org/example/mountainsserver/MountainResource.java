package org.example.mountainsserver;

// imports
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// resource RequestMapping to allow swagger to work.
@RestController
@RequestMapping("/resource")
public class MountainResource {
    private static ArrayList<Mountain> mountainList = new ArrayList<>();
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static int mountainId = 0;
    private ResponseEntity<Void> addDeleteResponse;
    private ResponseEntity<List<Mountain>> mountainsResponse;

    // record to create new mountains.
    private record Mountain(String name, String continent, String country, int height, int id){}

    /**
     * Adds a mountain to the mountains list.
     * @param newMountain The mountain being added.
     * @return Response code, based off if it is a valid mountain.
     */
    @PostMapping(value="/", consumes= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addMountain(@RequestBody final Mountain newMountain) {
        lock.writeLock().lock();
        if (newMountain == null || newMountain.name.isEmpty() || newMountain.continent.isEmpty()
                || newMountain.country.isEmpty() || newMountain.height < 0) {
            addDeleteResponse = ResponseEntity.badRequest().build();
        }  else if (checkForConflict(newMountain)) {
            addDeleteResponse = ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            // clone the new mountain, assigning a unique ID.
            Mountain mountain = new Mountain(newMountain.name, newMountain.continent,
                    newMountain.country, newMountain.height, mountainId++);

            mountainList.add(mountain);
            addDeleteResponse = ResponseEntity.ok().build();
        }
        lock.writeLock().unlock();
        return addDeleteResponse;
    }

    /**
     * Gets mountains based off of parameters inputted by the user.
     * @param continent The mountain's continent.
     * @param country The mountain's country.
     * @param name The mountain's name.
     * @param altitude The minimum altitude of the mountain.
     * @param id The ID of the mountain.
     * @return The response returned after attempting to get a mountain(s).
     */
    @GetMapping(value = {"/", "/{continent}", "/{continent}/{country}",
            "/{continent}/{country}/{name}", "/{continent}/{country}/{name}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Mountain>> getMountains(@PathVariable (name = "continent", required = false) final String continent,
                                                               @PathVariable (name = "country", required = false) final String country,
                                                               @PathVariable (name = "name", required = false) final String name,
                                                               @RequestParam (name = "altitude", required = false) final String altitude,
                                                               @RequestParam (name = "id", required = false) final Integer id) {
        lock.readLock().lock();
        final List<Mountain> filteredMountainList = filterMountainList(continent, country, name, altitude, id);
        // Get the filtered output.
        if (filteredMountainList.isEmpty()) {
            mountainsResponse = ResponseEntity.notFound().build();
        } else {
            mountainsResponse = ResponseEntity.ok(filteredMountainList);
        }
        lock.readLock().unlock();
        return mountainsResponse;
    }

    /**
     * Delete a mountain based on input.
     * @param continent The mountain's continent.
     * @param country The mountain's country.
     * @param name The mountain's name.
     * @param altitude The minimum altitude of the mountain.
     * @param id The ID of the mountain.
     * @return The response returned after attempting to delete a mountain.
     */
    @DeleteMapping("/")
    public ResponseEntity<Void> deleteMountain(@PathVariable (name = "continent", required = false) final String continent,
                                               @PathVariable (name = "country", required = false) final String country,
                                               @PathVariable (name = "name", required = false) final String name,
                                               @RequestParam (name = "altitude", required = false) final String altitude,
                                               @RequestParam (name = "id", required = false) final Integer id) {
       lock.writeLock().lock();
        final List<Mountain> filteredList = filterMountainList(continent, country, name, altitude, id);
        if (filteredList.isEmpty()) {
            addDeleteResponse = ResponseEntity.noContent().build();
        } else {
            mountainList.removeAll(filteredList);
            addDeleteResponse = ResponseEntity.ok().build();
        }
        lock.writeLock().unlock();
        return addDeleteResponse;
    }

    /**
     * Filter mountains for deleting or getting.
     * @param continent The desired mountain continent.
     * @param country The desired mountain country.
     * @param name The desired mountain name.
     * @param altitude The desired minimum altitude.
     * @param id The desired id.
     * @return A filtered mountain list.
     */
    private List<Mountain> filterMountainList(final String continent, final String country, final String name, final String altitude, final Integer id) {
        return mountainList.stream()
                .filter(continent == null ? mountain -> true : mountain -> mountain.continent.equals(continent))
                .filter(country == null ? mountain -> true : mountain -> mountain.country.equals(country))
                .filter(name == null ? mountain -> true : mountain -> mountain.name.equals(name))
                .filter(id == null ? mountain -> true : mountain -> mountain.id == id)
                .filter(altitude == null ? mountain -> true : mountain ->  mountain.height >= Integer.parseInt(altitude))
                .toList();
    }

    /**
     * Check if the mountain already exists in the list.
     * @param mountain The new mountain, mountain object.
     * @return true (if it does collide) or false otherwise.
     */
    private static Boolean checkForConflict(Mountain mountain) {
        // check if the mountain list contains the exact mountain.
        if (mountainList.contains(mountain)) {
            return true;
        } else {
            // check if there exists a mountain with the same name.
            for (Mountain existingMountain : mountainList) {
                if (existingMountain.name.equalsIgnoreCase(mountain.name)) {
                    return true;
                }
            }
        }
        return false;
    }
}

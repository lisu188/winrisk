package com.winrisk.web.api;

import com.winrisk.game.map.BuiltinMaps;
import com.winrisk.web.api.dto.AttackResponse;
import com.winrisk.web.api.dto.CreateGameRequest;
import com.winrisk.web.api.dto.GameSummary;
import com.winrisk.web.api.dto.GameView;
import com.winrisk.web.api.dto.TradeSetView;
import com.winrisk.web.game.GameService;
import com.winrisk.web.game.SaveStore;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService games;
    private final SaveStore saveStore;

    public GameController(GameService games, SaveStore saveStore) {
        this.games = games;
        this.saveStore = saveStore;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody CreateGameRequest request) {
        GameView view = games.create(request);
        return Map.of("id", view.id, "view", view);
    }

    @GetMapping("/games")
    public List<GameSummary> list() {
        return games.list();
    }

    @GetMapping("/games/{id}")
    public GameView get(@PathVariable String id) {
        return games.view(id);
    }

    @GetMapping("/games/{id}/background")
    public ResponseEntity<byte[]> background(@PathVariable String id) {
        byte[] image = games.backgroundImage(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(image);
    }

    @PostMapping("/games/{id}/place")
    public GameView place(@PathVariable String id, @RequestBody Map<String, Object> body) {
        int fieldIndex = intArg(body, "fieldIndex");
        boolean all = Boolean.TRUE.equals(body.get("all"));
        return games.place(id, fieldIndex, all);
    }

    @PostMapping("/games/{id}/attack")
    public AttackResponse attack(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return games.attack(id, intArg(body, "from"), intArg(body, "to"));
    }

    @PostMapping("/games/{id}/maneuver")
    public GameView maneuver(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return games.maneuver(id, intArg(body, "from"), intArg(body, "to"),
                intArg(body, "troops"));
    }

    @PostMapping("/games/{id}/end-phase")
    public GameView endPhase(@PathVariable String id) {
        return games.endPhase(id);
    }

    @GetMapping("/games/{id}/trade-sets")
    public List<TradeSetView> tradeSets(@PathVariable String id) {
        return games.tradeSets(id);
    }

    @PostMapping("/games/{id}/trade")
    public GameView trade(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("cardIndexes");
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("cardIndexes must be an array");
        }
        int[] indexes = list.stream()
                .mapToInt(item -> ((Number) item).intValue())
                .toArray();
        return games.trade(id, indexes);
    }

    @PostMapping("/games/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable String id, @RequestBody Map<String, Object> body) {
        games.save(id, stringArg(body, "name"));
    }

    @GetMapping("/saves")
    public List<String> saves() {
        return saveStore.listSaves();
    }

    @PostMapping("/games/load")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> load(@RequestBody Map<String, Object> body) {
        GameView view = games.load(stringArg(body, "name"));
        return Map.of("id", view.id, "view", view);
    }

    @GetMapping("/maps")
    public Map<String, Object> maps() {
        return Map.of("builtin", BuiltinMaps.names(), "files", saveStore.listMapFiles());
    }

    @DeleteMapping("/games/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        games.delete(id);
    }

    private int intArg(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        return number.intValue();
    }

    private String stringArg(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " must be a non-empty string");
        }
        return text;
    }
}

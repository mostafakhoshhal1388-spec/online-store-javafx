package ir.store.backend;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.Statement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final JdbcTemplate db;
    private final String adminKey;
    public ProductController(JdbcTemplate db, @Value("${store.admin-key}") String adminKey) {
        this.db = db;
        this.adminKey = adminKey;
    }

    @GetMapping
    public List<Product> all() {
        return db.query("SELECT * FROM products ORDER BY id DESC", (rs, n) -> new Product(
            rs.getLong("id"), rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"),
            rs.getInt("stock"), rs.getString("image_url")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestHeader(value = "X-Admin-Key", required = false) String key, @RequestBody ProductInput in) {
        requireAdmin(key);
        validate(in);
        KeyHolder keys = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO products(name,description,price,stock,image_url) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, in.name().trim());
            statement.setString(2, in.description());
            statement.setBigDecimal(3, in.price());
            statement.setInt(4, in.stock());
            statement.setString(5, in.imageUrl());
            return statement;
        }, keys);
        Number generatedKey = keys.getKey();
        if (generatedKey == null) throw new IllegalStateException("شناسهٔ کالای ذخیره‌شده از پایگاه داده دریافت نشد.");
        Long id = generatedKey.longValue();
        return new Product(id, in.name().trim(), in.description(), in.price(), in.stock(), in.imageUrl());
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @RequestHeader(value = "X-Admin-Key", required = false) String key, @RequestBody ProductInput in) {
        requireAdmin(key);
        validate(in);
        int changed = db.update("UPDATE products SET name=?,description=?,price=?,stock=?,image_url=? WHERE id=?",
            in.name().trim(), in.description(), in.price(), in.stock(), in.imageUrl(), id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return new Product(id, in.name().trim(), in.description(), in.price(), in.stock(), in.imageUrl());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        if (db.update("DELETE FROM products WHERE id=?", id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private void validate(ProductInput in) {
        if (in.name() == null || in.name().isBlank() || in.price() == null || in.price().compareTo(BigDecimal.ZERO) < 0 || in.stock() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "نام، قیمت معتبر و موجودی صفر یا بیشتر لازم است.");
    }
    private void requireAdmin(String key) {
        if (key == null || !java.security.MessageDigest.isEqual(
            adminKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            key.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "کلید مدیریت معتبر نیست.");
        }
    }
    public record ProductInput(String name, String description, BigDecimal price, int stock, String imageUrl) { }
}

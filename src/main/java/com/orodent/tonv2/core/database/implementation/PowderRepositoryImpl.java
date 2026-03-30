package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.repository.PowderRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PowderRepositoryImpl implements PowderRepository {

    private final Connection conn;

    public PowderRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Powder> findAll() {
        List<Powder> list = new ArrayList<>();

        String sql = "SELECT id, code, name, view_order, strength, translucency, yttria, notes FROM powder";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public Powder findById(int id) {
        String sql = "SELECT id, code, name, view_order, strength, translucency, yttria, notes FROM powder WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Powder> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, code, name, view_order, strength, translucency, yttria, notes FROM powder WHERE id IN (" + placeholders + ")";
        List<Powder> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public Powder save(Powder powder) {
        if (powder.id() == 0) {
            return insert(powder);
        } else {
            return update(powder);
        }
    }

    private Powder insert(Powder p) {
        String sql =
                "INSERT INTO powder(code, name, view_order, strength, translucency, yttria, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.code());
            ps.setString(2, p.name());
            ps.setInt(3, p.viewOrder());
            ps.setObject(4, p.strength());
            ps.setObject(5, p.translucency());
            ps.setObject(6, p.yttria());
            ps.setString(7, p.notes());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Powder(
                            keys.getInt(1),
                            p.code(),
                            p.name(),
                            p.viewOrder(),
                            p.strength(),
                            p.translucency(),
                            p.yttria(),
                            p.notes()
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return p;
    }

    private Powder update(Powder p) {
        String sql =
                "UPDATE powder SET code=?, name=?, view_order=?, strength=?, translucency=?, yttria=?, notes=? WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.code());
            ps.setString(2, p.name());
            ps.setInt(3, p.viewOrder());
            ps.setObject(4, p.strength());
            ps.setObject(5, p.translucency());
            ps.setObject(6, p.yttria());
            ps.setString(7, p.notes());
            ps.setInt(8, p.id());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return p;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM powder WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private Powder map(ResultSet rs) throws SQLException {
        return new Powder(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getInt("view_order"),
                rs.getDouble("strength"),
                rs.getDouble("translucency"),
                rs.getInt("yttria"),
                rs.getString("notes")
        );
    }
}

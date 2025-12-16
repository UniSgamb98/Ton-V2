package com.orodent.tonv2.core.database.implementation;


import com.orodent.tonv2.core.database.model.PowderOxide;
import com.orodent.tonv2.core.database.repository.PowderOxideRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PowderOxideRepositoryImpl implements PowderOxideRepository {

    private final Connection conn;

    public PowderOxideRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<PowderOxide> findByPowder(int powderId) {
        List<PowderOxide> list = new ArrayList<>();

        String sql = "SELECT id, powder_id, oxide_name, percentage FROM powder_oxide WHERE powder_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, powderId);

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
    public PowderOxide save(PowderOxide o) {
        if (o.id() == 0) return insert(o);
        return update(o);
    }

    private PowderOxide insert(PowderOxide o) {
        String sql = "INSERT INTO powder_oxide(powder_id, oxide_name, percentage) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, o.powderId());
            ps.setString(2, o.oxideName());
            ps.setDouble(3, o.percentage());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new PowderOxide(
                            keys.getInt(1),
                            o.powderId(),
                            o.oxideName(),
                            o.percentage()
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return o;
    }

    private PowderOxide update(PowderOxide o) {
        String sql = "UPDATE powder_oxide SET oxide_name=?, percentage=? WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.oxideName());
            ps.setDouble(2, o.percentage());
            ps.setInt(3, o.id());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return o;
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM powder_oxide WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private PowderOxide map(ResultSet rs) throws SQLException {
        return new PowderOxide(
                rs.getInt("id"),
                rs.getInt("powder_id"),
                rs.getString("oxide_name"),
                rs.getDouble("percentage")
        );
    }
}

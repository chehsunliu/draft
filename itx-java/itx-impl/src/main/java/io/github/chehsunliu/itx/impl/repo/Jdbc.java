package io.github.chehsunliu.itx.impl.repo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

public final class Jdbc {
    private Jdbc() {}

    @FunctionalInterface
    public interface ConnectionFn<T> {
        T apply(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface ConnectionVoidFn {
        void apply(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> T useConnection(DataSource ds, ConnectionFn<T> fn) {
        try (Connection conn = ds.getConnection()) {
            return fn.apply(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void useConnectionVoid(DataSource ds, ConnectionVoidFn fn) {
        useConnection(ds, conn -> {
            fn.apply(conn);
            return null;
        });
    }

    public static <T> T transactionally(DataSource ds, ConnectionFn<T> fn) {
        try (Connection conn = ds.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = fn.apply(conn);
                conn.commit();
                return result;
            } catch (Throwable t) {
                conn.rollback();
                throw t;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void bindAll(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    public static <T> List<T> mapAll(ResultSet rs, ResultSetMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        while (rs.next()) out.add(mapper.map(rs));
        return out;
    }

    public static <T> Optional<T> firstOrNull(ResultSet rs, ResultSetMapper<T> mapper) throws SQLException {
        return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
    }
}

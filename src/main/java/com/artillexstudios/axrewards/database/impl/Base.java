package com.artillexstudios.axrewards.database.impl;

import com.artillexstudios.axrewards.database.Database;
import com.artillexstudios.axrewards.guis.data.RewardsManager;
import com.artillexstudios.axrewards.guis.data.Reward;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Base implements Database {
    private QueryRunner runner;

    private final String CREATE_TABLE2 = """
            CREATE TABLE IF NOT EXISTS axrewards_players (
                id INT NOT NULL AUTO_INCREMENT,
                uuid VARCHAR(36) NOT NULL,
                name VARCHAR(128),
                PRIMARY KEY (id), UNIQUE (uuid)
            );
    """;

    private final String CREATE_TABLE3 = """
            CREATE TABLE IF NOT EXISTS axrewards_rewards (
                id INT NOT NULL AUTO_INCREMENT,
                name VARCHAR(128) NOT NULL,
                PRIMARY KEY (id), UNIQUE (name)
            );
    """;

    private final String CREATE_TABLE4 = """
            CREATE TABLE IF NOT EXISTS axrewards_cooldowns (
                id INT NOT NULL AUTO_INCREMENT,
                player_id INT NOT NULL,
                reward_id INT NOT NULL,
                time BIGINT NOT NULL,
                PRIMARY KEY (id)
            );
    """;

    private final String SELECT_PLAYER_BY_UUID = """
            SELECT id FROM axrewards_players WHERE uuid = ?
    """;

    private final String INSERT_PLAYER = """
            INSERT INTO axrewards_players (uuid, name) VALUES (?, ?)
    """;

    private final String INSERT_REWARD = """
            INSERT INTO axrewards_rewards (name) VALUES (?, ?)
    """;

    private final String SELECT_REWARD = """
            SELECT id FROM axrewards_rewards WHERE name = ?
    """;

    private final String LAST_CLAIM = """
            SELECT time FROM axrewards_cooldowns WHERE player_id = ? AND reward_id = ? LIMIT 1;
    """;

    private final String CLAIM_REWARD = """
            INSERT INTO axrewards_cooldowns (player_id, reward_id, time) VALUES (?, ?, ?)
    """;

    private final String RESET_REWARD_SPECIFIC = """
            DELETE FROM axrewards_cooldowns WHERE player_id = ? AND reward_id = ?
    """;

    private final String RESET_REWARD_ALL = """
            DELETE FROM axrewards_cooldowns WHERE player_id = ?
    """;

    public abstract Connection getConnection();

    @Override
    public abstract String getType();

    public QueryRunner getRunner() {
        return runner;
    }

    @Override
    public void setup() {
        runner = new QueryRunner();

        try (Connection conn = getConnection()) {
            runner.execute(conn, CREATE_TABLE2);
            runner.execute(conn, CREATE_TABLE3);
            runner.execute(conn, CREATE_TABLE4);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        reload();
    }

    @Override
    public void reload() {
        try (Connection conn = getConnection()) {
            for (Reward reward : RewardsManager.getRewards()) {
                try {
                    runner.execute(conn, INSERT_REWARD, reward.name());
                } catch (Exception ignored) {}
            }
        } catch (SQLException ex) {
            // ignore errors caused by data already existing
        }
    }

    @Override
    public int getPlayerId(OfflinePlayer player) {
        ScalarHandler<Number> scalarHandler = new ScalarHandler<>();
        try (Connection conn = getConnection()) {
            Number id = runner.query(conn, SELECT_PLAYER_BY_UUID, scalarHandler, player.getUniqueId().toString());
            if (id != null) return id.intValue();
            return runner.insert(conn, INSERT_PLAYER, scalarHandler, player.getUniqueId().toString(), player.getName()).intValue();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Could not create user in database!");
    }

    @Override
    public int getRewardId(Reward reward) {
        ScalarHandler<Integer> scalarHandler = new ScalarHandler<>();
        try (Connection conn = getConnection()) {
            return runner.query(conn, SELECT_REWARD, scalarHandler, reward.name());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        throw new RuntimeException("Could not find reward " + reward.name() + " in database!");
    }

    @Override
    public long getLastClaim(OfflinePlayer player, Reward reward) {
        ScalarHandler<Long> scalarHandler = new ScalarHandler<>();
        try (Connection conn = getConnection()) {
            Long n = runner.query(conn, LAST_CLAIM, scalarHandler, getPlayerId(player), getRewardId(reward));
            if (n != null) return n;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return -1; // something went wrong
        }
        return 0; // never claimed it
    }

    @Override
    public void claimReward(OfflinePlayer player, Reward reward) {
        claimReward(player, reward, System.currentTimeMillis());
    }

    public void claimReward(OfflinePlayer player, Reward reward, long time) {
        resetReward(player, reward);
        try (Connection conn = getConnection()) {
            runner.execute(conn, CLAIM_REWARD, getPlayerId(player), getRewardId(reward), time);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void resetReward(OfflinePlayer player, Reward reward) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, RESET_REWARD_SPECIFIC, getPlayerId(player), getRewardId(reward));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void resetReward(OfflinePlayer player) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, RESET_REWARD_ALL, getPlayerId(player));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public abstract void disable();
}

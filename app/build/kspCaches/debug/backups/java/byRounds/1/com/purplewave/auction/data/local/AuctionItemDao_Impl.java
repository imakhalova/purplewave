package com.purplewave.auction.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.purplewave.auction.domain.SyncStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AuctionItemDao_Impl implements AuctionItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AuctionItemEntity> __insertionAdapterOfAuctionItemEntity;

  private final SyncStatusConverter __syncStatusConverter = new SyncStatusConverter();

  private final EntityDeletionOrUpdateAdapter<AuctionItemEntity> __deletionAdapterOfAuctionItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSyncState;

  public AuctionItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAuctionItemEntity = new EntityInsertionAdapter<AuctionItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `auction_items` (`id`,`title`,`description`,`condition_rating`,`photo_uri`,`sync_status`,`captured_at_ms`,`sync_attempts`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AuctionItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindLong(4, entity.getConditionRating());
        if (entity.getPhotoUri() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPhotoUri());
        }
        final String _tmp = __syncStatusConverter.fromSyncStatus(entity.getSyncStatus());
        statement.bindString(6, _tmp);
        statement.bindLong(7, entity.getCapturedAtMs());
        statement.bindLong(8, entity.getSyncAttempts());
      }
    };
    this.__deletionAdapterOfAuctionItemEntity = new EntityDeletionOrUpdateAdapter<AuctionItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `auction_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AuctionItemEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateSyncState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE auction_items SET sync_status = ?, sync_attempts = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AuctionItemEntity item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAuctionItemEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final AuctionItemEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAuctionItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSyncState(final long id, final SyncStatus status, final int attempts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSyncState.acquire();
        int _argIndex = 1;
        final String _tmp = __syncStatusConverter.fromSyncStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, attempts);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSyncState.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AuctionItemEntity>> observeAll() {
    final String _sql = "SELECT * FROM auction_items ORDER BY captured_at_ms DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"auction_items"}, new Callable<List<AuctionItemEntity>>() {
      @Override
      @NonNull
      public List<AuctionItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfConditionRating = CursorUtil.getColumnIndexOrThrow(_cursor, "condition_rating");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_uri");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfCapturedAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "captured_at_ms");
          final int _cursorIndexOfSyncAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_attempts");
          final List<AuctionItemEntity> _result = new ArrayList<AuctionItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuctionItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final int _tmpConditionRating;
            _tmpConditionRating = _cursor.getInt(_cursorIndexOfConditionRating);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final SyncStatus _tmpSyncStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __syncStatusConverter.toSyncStatus(_tmp);
            final long _tmpCapturedAtMs;
            _tmpCapturedAtMs = _cursor.getLong(_cursorIndexOfCapturedAtMs);
            final int _tmpSyncAttempts;
            _tmpSyncAttempts = _cursor.getInt(_cursorIndexOfSyncAttempts);
            _item = new AuctionItemEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpConditionRating,_tmpPhotoUri,_tmpSyncStatus,_tmpCapturedAtMs,_tmpSyncAttempts);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPendingOrFailed(
      final Continuation<? super List<AuctionItemEntity>> $completion) {
    final String _sql = "SELECT * FROM auction_items WHERE sync_status IN ('PENDING','FAILED') ORDER BY captured_at_ms ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AuctionItemEntity>>() {
      @Override
      @NonNull
      public List<AuctionItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfConditionRating = CursorUtil.getColumnIndexOrThrow(_cursor, "condition_rating");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_uri");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfCapturedAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "captured_at_ms");
          final int _cursorIndexOfSyncAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_attempts");
          final List<AuctionItemEntity> _result = new ArrayList<AuctionItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuctionItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final int _tmpConditionRating;
            _tmpConditionRating = _cursor.getInt(_cursorIndexOfConditionRating);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final SyncStatus _tmpSyncStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __syncStatusConverter.toSyncStatus(_tmp);
            final long _tmpCapturedAtMs;
            _tmpCapturedAtMs = _cursor.getLong(_cursorIndexOfCapturedAtMs);
            final int _tmpSyncAttempts;
            _tmpSyncAttempts = _cursor.getInt(_cursorIndexOfSyncAttempts);
            _item = new AuctionItemEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpConditionRating,_tmpPhotoUri,_tmpSyncStatus,_tmpCapturedAtMs,_tmpSyncAttempts);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final long id, final Continuation<? super AuctionItemEntity> $completion) {
    final String _sql = "SELECT * FROM auction_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AuctionItemEntity>() {
      @Override
      @Nullable
      public AuctionItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfConditionRating = CursorUtil.getColumnIndexOrThrow(_cursor, "condition_rating");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_uri");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfCapturedAtMs = CursorUtil.getColumnIndexOrThrow(_cursor, "captured_at_ms");
          final int _cursorIndexOfSyncAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_attempts");
          final AuctionItemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final int _tmpConditionRating;
            _tmpConditionRating = _cursor.getInt(_cursorIndexOfConditionRating);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final SyncStatus _tmpSyncStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfSyncStatus);
            _tmpSyncStatus = __syncStatusConverter.toSyncStatus(_tmp);
            final long _tmpCapturedAtMs;
            _tmpCapturedAtMs = _cursor.getLong(_cursorIndexOfCapturedAtMs);
            final int _tmpSyncAttempts;
            _tmpSyncAttempts = _cursor.getInt(_cursorIndexOfSyncAttempts);
            _result = new AuctionItemEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpConditionRating,_tmpPhotoUri,_tmpSyncStatus,_tmpCapturedAtMs,_tmpSyncAttempts);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

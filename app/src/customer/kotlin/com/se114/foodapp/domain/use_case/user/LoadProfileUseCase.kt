package com.se114.foodapp.domain.use_case.user

import com.example.foodapp.data.model.Account
import com.example.foodapp.domain.repository.AccountRepository
import com.example.foodapp.domain.use_case.auth.FirebaseResult
import com.example.foodapp.utils.StringUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoadProfileUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<FirebaseResult<Account>> = flow {
        emit(FirebaseResult.Loading)

        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser == null) {
            emit(FirebaseResult.Failure("Người dùng chưa đăng nhập"))
            return@flow
        }

        try {
            val uid = firebaseUser.uid
            val docRef = Firebase.firestore.collection("users").document(uid)
            var userDoc = docRef.get().await()

            // Nếu user chưa có document (lần đầu login Google)
            if (!userDoc.exists()) {
                val newUser = mapOf(
                    "id" to firebaseUser.uid,
                    "displayName" to (firebaseUser.displayName ?: ""),
                    "email" to (firebaseUser.email ?: ""),
                    "avatar" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "phoneNumber" to "",
                    "gender" to "",
                    "dob" to ""
                )

                docRef.set(newUser).await()
                userDoc = docRef.get().await() // Reload sau khi tạo
            }

            // Map dữ liệu từ Firestore
            val account = Account(
                displayName = userDoc.getString("displayName") ?: "",
                email = userDoc.getString("email") ?: "",
                avatar = userDoc.getString("avatar") ?: "",
                phoneNumber = userDoc.getString("phoneNumber") ?: "",
                gender = userDoc.getString("gender") ?: "",
                dob = userDoc.getString("dob")?.let { StringUtils.parseLocalDate(it) }
            )

            emit(FirebaseResult.Success(account))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(FirebaseResult.Failure("Không thể tải thông tin người dùng: ${e.localizedMessage}"))
        }

    }.flowOn(Dispatchers.IO)


}
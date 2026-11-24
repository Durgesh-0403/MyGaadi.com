package com.mygaadi.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password", "sellerProfile", "buyerProfile", "adminProfile"})
public class User extends BaseClass {

	@Column(name = "first_name", length = 60, nullable = false)
	private String firstName;

	@Column(name = "last_name", length = 60, nullable = false)
	private String lastName;

	@Column(name = "email", length = 120, nullable = false, unique = true)
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@Column(name = "phone", length = 20, nullable = false, unique = true)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_role", length = 30, nullable = false)
	private UserRole userRole;

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private boolean active = true;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	private SellerProfile sellerProfile;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	private BuyerProfile buyerProfile;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	private AdminProfile adminProfile;

	public String getCompleteName() {
		return firstName + " " + lastName;
	}

	public void setSellerProfile(SellerProfile sellerProfile) {
		this.sellerProfile = sellerProfile;
		if (sellerProfile != null) {
			sellerProfile.setUser(this);
		}
	}

	public void setBuyerProfile(BuyerProfile buyerProfile) {
		this.buyerProfile = buyerProfile;
		if (buyerProfile != null) {
			buyerProfile.setUser(this);
		}
	}

	public void setAdminProfile(AdminProfile adminProfile) {
		this.adminProfile = adminProfile;
		if (adminProfile != null) {
			adminProfile.setUser(this);
		}
	}
}

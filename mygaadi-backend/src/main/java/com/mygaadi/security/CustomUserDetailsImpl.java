package com.mygaadi.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mygaadi.entities.User;
import com.mygaadi.entities.UserRole;

import lombok.Getter;

@Getter
public class CustomUserDetailsImpl implements UserDetails {

	private final Long userId;
	private final String email;
	private final String password;
	private final UserRole role;
	private final boolean active;
	private final String completeName;
	private final Collection<? extends GrantedAuthority> authorities;

	public CustomUserDetailsImpl(User user) {
		this.userId = user.getId();
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.role = user.getUserRole();
		this.active = user.isActive();
		this.completeName = user.getCompleteName();
		this.authorities = List.of(new SimpleGrantedAuthority(user.getUserRole().name()));
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return active;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}

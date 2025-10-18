package com.anil.expensetracker.expensetracker.service;

import com.anil.expensetracker.expensetracker.dto.GroupRequest;
import com.anil.expensetracker.expensetracker.model.Group;
import com.anil.expensetracker.expensetracker.model.GroupMember;
import com.anil.expensetracker.expensetracker.model.User;
import com.anil.expensetracker.expensetracker.repository.GroupMemberRepository;
import com.anil.expensetracker.expensetracker.repository.GroupRepository;
import com.anil.expensetracker.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    
    public Group createGroup(GroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as a member
        addMemberToGroup(savedGroup.getId(), creatorId);
        
        return savedGroup;
    }
    
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }
    
    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }
    
    public List<Group> getGroupsByUserId(Long userId) {
        return groupRepository.findByUserId(userId);
    }
    
    public Group updateGroup(Long id, GroupRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        
        return groupRepository.save(group);
    }
    
    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new IllegalArgumentException("Group not found");
        }
        groupRepository.deleteById(id);
    }
    
    public GroupMember addMemberToGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(groupId, userId)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }
        
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setIsActive(true);
        
        return groupMemberRepository.save(member);
    }
    
    public void removeMemberFromGroup(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findActiveMemberByGroupAndUser(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group"));
        
        member.setIsActive(false);
        groupMemberRepository.save(member);
    }
    
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupIdAndIsActiveTrue(groupId);
    }
}
